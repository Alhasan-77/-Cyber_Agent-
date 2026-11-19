"use strict";

const crypto = require("node:crypto");
const { initializeApp, getApps } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getAppCheck } = require("firebase-admin/app-check");
const { Firestore, FieldValue } = require("@google-cloud/firestore");
const { defineSecret, defineString } = require("firebase-functions/params");
const { onInit } = require("firebase-functions/v2/core");
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
const DAILY_REQUEST_LIMIT = 50;
let consumeDailyQuota;

// Parameter values are unavailable while Firebase discovers functions at deploy time.
// Initialize the named database only when the runtime instance starts.
onInit(() => {
  const databaseId = FIRESTORE_DATABASE_ID.value();
  if (!databaseId || databaseId === "(default)") {
    throw new Error("FIRESTORE_DATABASE_ID must identify the configured named Firestore database.");
  }
  const quotaDb = new Firestore({ databaseId });
  consumeDailyQuota = require("./src/quota").createDailyQuotaConsumer(quotaDb, {
    dailyLimit: DAILY_REQUEST_LIMIT,
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
      if (typeof consumeDailyQuota !== "function") {
        throw new Error("OpenAI quota storage is not initialized.");
      }
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
