"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const crypto = require("node:crypto");
const { Firestore } = require("@google-cloud/firestore");
const { createDailyQuotaConsumer } = require("../src/quota");

const DATABASE_ID = "ai-studio-android-novaforg-6912163c-aa3c-4903-a89e-6df061c893e0";
const emulatorAvailable = Boolean(process.env.FIRESTORE_EMULATOR_HOST);

test("OpenAI quota is isolated in the configured named Firestore database", {
  skip: !emulatorAvailable && "Requires FIRESTORE_EMULATOR_HOST; run via firebase emulators:exec.",
}, async () => {
  const projectId = process.env.GCLOUD_PROJECT || process.env.GCP_PROJECT || "demo-no-project";
  const namedDb = new Firestore({ projectId, databaseId: DATABASE_ID });
  const defaultDb = new Firestore({ projectId });
  const uid = `quota-test-${crypto.randomUUID()}`;
  const uidHash = crypto.createHash("sha256").update(uid).digest("hex");
  let currentTime = new Date("2026-11-19T12:00:00.000Z");

  try {
    const consume = createDailyQuotaConsumer(namedDb, {
      dailyLimit: 2,
      now: () => currentTime,
    });

    await consume(uid);
    await consume(uid);
    await assert.rejects(consume(uid), (error) => error.statusCode === 429);

    const namedSnapshot = await namedDb.collection("_openaiUsage").doc(uidHash).get();
    assert.equal(namedSnapshot.exists, true);
    assert.equal(namedSnapshot.get("requestCount"), 2);
    assert.equal(namedSnapshot.get("day"), "2026-11-19");

    const defaultSnapshot = await defaultDb.collection("_openaiUsage").doc(uidHash).get();
    assert.equal(defaultSnapshot.exists, false, "quota must never be written to the default database");

    currentTime = new Date("2026-11-20T00:00:01.000Z");
    await consume(uid);
    const nextDaySnapshot = await namedDb.collection("_openaiUsage").doc(uidHash).get();
    assert.equal(nextDaySnapshot.get("requestCount"), 1);
    assert.equal(nextDaySnapshot.get("day"), "2026-11-20");

    await consume(`${uid}-second-user`);
    const docs = await namedDb.collection("_openaiUsage").get();
    assert.ok(docs.docs.some((doc) => doc.id !== uidHash), "quota records should be isolated by user");
  } finally {
    await Promise.all([namedDb.terminate(), defaultDb.terminate()]);
  }
});

test("quota consumer rejects an unavailable Firestore transaction (fail closed)", async () => {
  const failingDb = {
    collection: () => ({ doc: () => ({}) }),
    runTransaction: async () => {
      throw new Error("simulated Firestore outage");
    },
  };
  const consume = createDailyQuotaConsumer(failingDb);
  await assert.rejects(consume("fail-closed-user"), /simulated Firestore outage/);
});

test("quota consumer rejects invalid database clients and user IDs", async () => {
  assert.throws(() => createDailyQuotaConsumer(null), /valid Firestore database client/);
  const consume = createDailyQuotaConsumer({
    collection: () => ({ doc: () => ({}) }),
    runTransaction: async () => {},
  });
  await assert.rejects(consume(" "), /valid user ID/);
});
