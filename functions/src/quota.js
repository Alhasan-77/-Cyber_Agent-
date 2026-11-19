"use strict";

const crypto = require("node:crypto");
const { FieldValue } = require("@google-cloud/firestore");

function createDailyQuotaConsumer(db, options = {}) {
  if (!db || typeof db.collection !== "function" || typeof db.runTransaction !== "function") {
    throw new TypeError("A valid Firestore database client is required.");
  }

  const dailyLimit = options.dailyLimit ?? 50;
  const now = options.now ?? (() => new Date());
  if (!Number.isInteger(dailyLimit) || dailyLimit < 1) {
    throw new TypeError("dailyLimit must be a positive integer.");
  }
  if (typeof now !== "function") {
    throw new TypeError("now must be a function.");
  }

  return async function consumeDailyQuota(uid) {
    if (typeof uid !== "string" || uid.trim().length === 0) {
      throw new TypeError("A valid user ID is required.");
    }

    const day = now().toISOString().slice(0, 10);
    const uidHash = crypto.createHash("sha256").update(uid).digest("hex");
    // Keep one quota document per user instead of creating one document per day.
    const usageRef = db.collection("_openaiUsage").doc(uidHash);

    await db.runTransaction(async (transaction) => {
      const snapshot = await transaction.get(usageRef);
      const storedDay = snapshot.exists ? snapshot.get("day") : null;
      const currentCount = storedDay === day
        ? Number(snapshot.get("requestCount") || 0)
        : 0;

      if (currentCount >= dailyLimit) {
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
  };
}

module.exports = { createDailyQuotaConsumer };
