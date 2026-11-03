const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
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

// Test 1: Unauthenticated user cannot read or write user profile
test("Unauthenticated: cannot read user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

// Test 2: Alice can create and read her own profile
test("Authenticated: Alice can create and read her own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  
  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      displayName: "Alice",
      currentStreak: 1,
      bestStreak: 1,
      totalScreenMinutesSaved: 30,
      todayProductivityScore: 85,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(profileRef.get());
});

// Test 3: Bob cannot read Alice's profile
test("Cross-user: Bob cannot read Alice's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      currentStreak: 1,
      bestStreak: 1,
      totalScreenMinutesSaved: 30,
      todayProductivityScore: 85,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

// Test 4: Alice can manage her app limits
test("AppLimits: Alice can create and read her app limit", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const limitRef = aliceDb.collection("users").doc(ALICE_UID).collection("app_limits").doc("instagram");

  await assertSucceeds(
    limitRef.set({
      userId: ALICE_UID,
      packageName: "com.instagram.android",
      appName: "Instagram",
      dailyBudgetMinutes: 30,
      blockReels: true,
      blockShorts: false,
      isBlocked: false,
      cooldownSeconds: 60,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(limitRef.get());
});

// Test 5: Bob cannot read Alice's app limits
test("Cross-user: Bob cannot read Alice's app limits", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("app_limits").doc("instagram").set({
      userId: ALICE_UID,
      packageName: "com.instagram.android",
      appName: "Instagram",
      dailyBudgetMinutes: 30,
      blockReels: true,
      blockShorts: false,
      isBlocked: false,
      cooldownSeconds: 60,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("app_limits").doc("instagram").get());
});
