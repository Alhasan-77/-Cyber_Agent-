const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validProjectPayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    name: "Nova Cloud App",
    description: "Full-stack AI application",
    language: "Full-Stack Web",
    htmlCode: "<div>Hello</div>",
    cssCode: "body { background: #000; }",
    jsCode: "console.log('ready');",
    backendCode: "fun main() {}",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read projects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/projects`).where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: can create and query own projects with matching filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/projects`).doc("proj_1").set(validProjectPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/projects`).where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: fails query without userId filter (rules are not filters)", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection(`users/${ALICE_UID}/projects`).doc("proj_1").set(validProjectPayload(ALICE_UID));
  });
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/projects`).get()
  );
});

test("Authenticated user: cannot read another user's project", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection(`users/${BOB_UID}/projects`).doc("bob_proj").set(validProjectPayload(BOB_UID));
  });
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection(`users/${BOB_UID}/projects`).doc("bob_proj").get()
  );
});

test("Shadow update test: rejects unauthorized ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const payload = validProjectPayload(ALICE_UID);
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/projects`).doc("proj_1").set(payload)
  );
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/projects`).doc("proj_1").update({
      name: "Updated Name",
      isVerifiedAdmin: true,
      updatedAt: new Date(),
    })
  );
});

test("Immutable snapshot test: allows create by owner, rejects update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const snapPayload = {
    userId: ALICE_UID,
    projectId: "proj_1",
    commitMessage: "Initial full-stack commit",
    versionTag: "v1.0.0",
    htmlCode: "<main></main>",
    cssCode: "",
    jsCode: "",
    backendCode: "",
    addedLines: 42,
    deletedLines: 0,
    createdAt: new Date(),
  };
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/snapshots`).doc("snap_1").set(snapPayload)
  );
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/snapshots`).doc("snap_1").update({
      commitMessage: "Tampered history",
    })
  );
});
