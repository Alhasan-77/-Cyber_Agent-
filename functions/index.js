"use strict";

const crypto = require("node:crypto");
const { initializeApp, getApps } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getAppCheck } = require("firebase-admin/app-check");
const { FieldValue, getFirestore } = require("firebase-admin/firestore");
const { defineSecret } = require("firebase-functions/params");
const { onRequest } = require("firebase-functions/v2/https");
const { logger } = require("firebase-functions");
const { validateOpenAiRequest } = require("./src/validation");

if (getApps().length === 0) {
  initializeApp();
}

const OPENAI_API_KEY = defineSecret("OPENAI_API_KEY");
const FIRESTORE_DATABASE_ID = defineString("FIRESTORE_DATABASE_ID", {
  default: "ai-studio-android-novaforg-6912163c-aa3c-4903-a89e-6df061c893e0",
});
const quotaDb = getFirestore(getApps()[0], FIRESTORE_DATABASE_ID.value());
const DAILY_REQUEST_LIMIT = 50;

async function consumeDailyQuota(uid) {
  const day = new Date().toISOString().slice(0, 10);
  const uidHash = crypto.createHash("sha256").update(uid).digest("hex");
  // Keep one quota document per user instead of creating a new document every day.
  const usageRef = quotaDb.collection("_openaiUsage").doc(uidHash);

  await quotaDb.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(usageRef);
    const storedDay = snapshot.exists ? snapshot.get("day") : null;
    const currentCount = storedDay === day
      ? Number(snapshot.get("requestCount") || 0)
      : 0;
    if (currentCount >= DAILY_REQUEST_LIMIT) {
      const error = new Error("Daily ChatGPT request limit reached.");
      error.statusCode = 429;
      throw error;
    }
    transaction.set(usageRef, {
      day,
      requestCount: currentCount + 1,
      updatedAt: FieldValue.serverTimestamp(),
    });
  });
}

exports.openaiChatCompletion = onRequest(
  {
    region: "us-central1",
    timeoutSeconds: 120,
    memory: "256MiB",
    maxInstances: 5,
    secrets: [OPENAI_API_KEY],
    cors: false,
  },
  async (request, response) => {
    response.set("Cache-Control", "no-store, max-age=0");
    response.set("X-Content-Type-Options", "nosniff");
    response.set("Referrer-Policy", "no-referrer");

    if (request.method !== "POST") {
      response.set("Allow", "POST");
      return response.status(405).json({ error: { message: "Method not allowed." } });
    }

    const authorization = request.get("Authorization") || "";
    const idToken = authorization.startsWith("Bearer ") ? authorization.slice(7).trim() : "";
    if (!idToken) {
      return response.status(401).json({ error: { message: "Authentication required." } });
    }

    let decodedToken;
    try {
      decodedToken = await getAuth().verifyIdToken(idToken, true);
    } catch {
      return response.status(401).json({ error: { message: "Authentication token is invalid or expired." } });
    }

    const appCheckToken = request.get("X-Firebase-AppCheck") || "";
    if (!appCheckToken) {
      return response.status(401).json({ error: { message: "App Check token required." } });
    }
    try {
      await getAppCheck().verifyToken(appCheckToken);
    } catch {
      return response.status(401).json({ error: { message: "App Check token is invalid or expired." } });
    }

    let payload;
    try {
      const bodyBytes = Buffer.byteLength(JSON.stringify(request.body ?? {}), "utf8");
      if (bodyBytes > 50000) {
        return response.status(413).json({ error: { message: "Request body is too large." } });
      }
      payload = validateOpenAiRequest(request.body);
    } catch (error) {
      return response.status(400).json({
        error: { message: error instanceof Error ? error.message : "Invalid request." },
      });
    }

    try {
      await consumeDailyQuota(decodedToken.uid);
    } catch (error) {
      const status = error && error.statusCode === 429 ? 429 : 503;
      if (status === 503) {
        logger.error("Unable to apply OpenAI daily quota check.", { uid: decodedToken.uid });
      }
      return response.status(status).json({
        error: {
          message: status === 429
            ? "Daily ChatGPT request limit reached. Try again tomorrow."
            : "ChatGPT service is temporarily unavailable.",
        },
      });
    }

    try {
      const upstream = await fetch("https://api.openai.com/v1/chat/completions", {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${OPENAI_API_KEY.value()}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
        signal: AbortSignal.timeout(90000),
      });

      const result = await upstream.json().catch(() => null);
      if (!upstream.ok) {
        logger.warn("OpenAI upstream rejected a request.", {
          uid: decodedToken.uid,
          status: upstream.status,
        });
        const message = typeof result?.error?.message === "string"
          ? result.error.message.slice(0, 500)
          : "OpenAI provider request failed.";
        return response.status(upstream.status === 429 ? 429 : 502)
          .json({ error: { message } });
      }

      if (!result || !Array.isArray(result.choices)) {
        return response.status(502).json({
          error: { message: "OpenAI provider returned an invalid response." },
        });
      }
      return response.status(200).json(result);
    } catch (error) {
      logger.error("OpenAI proxy request failed.", {
        uid: decodedToken.uid,
        errorName: error instanceof Error ? error.name : "UnknownError",
      });
      return response.status(502).json({
        error: { message: "ChatGPT provider is currently unreachable. Retry later." },
      });
    }
  }
);
