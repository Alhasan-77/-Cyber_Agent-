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

function validUserProfilePayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    email: "alice@novaforge.dev",
    displayName: "Alice Researcher",
    photoUrl: "https://example.com/avatar.png",
    activeBranch: "main",
    projectsCount: 2,
    bioExperimentsCount: 3,
    createdAt: now,
    updatedAt: now,
  };
}

function validBioExperimentPayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    title: "Hemoglobin Beta Folding",
    workflowMode: "PROTEIN_FOLDING",
    fastaSequence: ">HBB\nMVHLTPEEKSAVTALWGKVNVDEVGGEALGRLLVVYPWTQRFFESFGDLSTPDAVMGNPKVKAHGKKVLGAFSDGLAHLDNLKGTFATLSELHCDKLHVDPENFRLLGNVLVCVLAHHFGKEFTPPVQAAYQKVVAGVANALAHKYH",
    researchGoal: "Analyze 3D Alpha-Helix stability and pLDDT score",
    plddtScore: 94,
    freeEnergyKcal: -11.4,
    alphaHelixPct: 68,
    betaSheetPct: 14,
    aiReportSummary: "High structural confidence across heme-binding pocket.",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read projects, profiles, or bio experiments", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/projects`).where("userId", "==", ALICE_UID).get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).get()
  );
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/bio_experiments`).where("userId", "==", ALICE_UID).get()
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

test("Authenticated user: cannot read another user's project, profile, or bio experiment", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection(`users/${BOB_UID}/projects`).doc("bob_proj").set(validProjectPayload(BOB_UID));
    await context.firestore().collection("users").doc(BOB_UID).set(validUserProfilePayload(BOB_UID));
    await context.firestore().collection(`users/${BOB_UID}/bio_experiments`).doc("bob_exp").set(validBioExperimentPayload(BOB_UID));
  });
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection(`users/${BOB_UID}/projects`).doc("bob_proj").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).get()
  );
  await assertFails(
    aliceDb.collection(`users/${BOB_UID}/bio_experiments`).doc("bob_exp").get()
  );
});

test("Authenticated user: can create, read, and update own UserProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserProfilePayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      projectsCount: 5,
      bioExperimentsCount: 4,
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can create, query, and update own BioExperimentRecord", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/bio_experiments`).doc("exp_1").set(validBioExperimentPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/bio_experiments`).where("userId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/bio_experiments`).doc("exp_1").update({
      plddtScore: 97,
      updatedAt: new Date(),
    })
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
