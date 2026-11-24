const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated users are denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .set({
        userId: ALICE_UID,
        title: "Unauthorized",
        projectType: "TEXT_TO_VIDEO",
        prompt: "Cyberpunk city",
        style: "Cinematic",
        aspectRatio: "16:9",
        resolution: "720p",
        status: "QUEUED",
        creditCost: 10,
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("User profile creation enforces initial FREE tier, 50 credits, and owner isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Creator",
      email: "alice@example.com",
      planTier: "FREE",
      credits: 50,
      totalGenerations: 0,
      preferredLanguage: "id",
      darkTheme: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject self-upgrade to UNLIMITED or inflated credits on create
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob Hack",
      email: "bob@example.com",
      planTier: "UNLIMITED",
      credits: 9999,
      totalGenerations: 0,
      preferredLanguage: "en",
      darkTheme: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject cross-user profile read (PII protection)
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Reject listing /users collection
  await assertFails(aliceDb.collection("users").get());
});

test("User profile updates allow bounded credit changes and preferences, deny planTier tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Creator",
      email: "alice@example.com",
      planTier: "FREE",
      credits: 50,
      totalGenerations: 0,
      preferredLanguage: "id",
      darkTheme: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid preference update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "en",
      darkTheme: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Valid credit deduction
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      credits: 40,
      totalGenerations: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject planTier tampering
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      planTier: "PRO",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject excessive credit grant (> +10)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      credits: 500,
      totalGenerations: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Owner can create, list, update, and delete StudioProjects; cross-user & ghost fields denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Empty subcollection list must succeed for owner
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );

  // Create project without @DocumentId `id` field
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .set({
        userId: ALICE_UID,
        title: "Neon Drone Intro",
        projectType: "TEXT_TO_VIDEO",
        prompt: "Futuristic neon Tokyo aerial drone shot at night",
        style: "Cinematic",
        aspectRatio: "16:9",
        resolution: "720p",
        status: "PROCESSING",
        creditCost: 10,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Transition PROCESSING -> COMPLETED
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .update({
        status: "COMPLETED",
        localMediaPath: "/data/user/0/com.vidgenai.studio/files/vid_1.mp4",
        interactionId: "v1_interaction_abc",
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Post-completion subtitle & edit update succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .update({
        subtitlesJson: "[{\"start\":\"00:00\",\"end\":\"00:03\",\"text\":\"Welcome\"}]",
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Reject regressing status after COMPLETED
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .update({
        status: "QUEUED",
        updatedAt: new Date(Date.now() - 200),
      })
  );

  // Reject ghost field `id` on create
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_2")
      .set({
        id: "proj_2",
        userId: ALICE_UID,
        title: "Bad Project",
        projectType: "IMAGE_GEN",
        prompt: "Sunset",
        style: "Realistic",
        aspectRatio: "1:1",
        resolution: "1K",
        status: "COMPLETED",
        creditCost: 3,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject cross-user list, read, write, delete
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .delete()
  );

  // Owner delete succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .delete()
  );
});
