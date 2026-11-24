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
const ADMIN_UID = "admin_sahil";
const ADMIN_EMAIL = "sahiljutt060@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

function futureDate(msOffset = 7 * 24 * 60 * 60 * 1000) {
  return new Date(Date.now() + msOffset);
}

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("resources").where("published", "==", true).where("archived", "==", false).get());
  await assertFails(unauthDb.collection("courses").where("published", "==", true).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("paymentRequests").doc("pay_1").get());
});

test("User registration, profile edit, and trial anti-abuse rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  const now = pastDate(5000);
  // Valid normal user creation
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      fullName: "Alice Khan",
      email: "alice@example.com",
      role: "USER",
      accountStatus: "ACTIVE",
      preferredLanguage: "EN",
      trialStatus: "NOT_STARTED",
      subscriptionStatus: "NONE",
      createdAt: now,
      updatedAt: now,
    })
  );

  // User cannot self-escalate role
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "SUPER_ADMIN",
      updatedAt: pastDate(1000),
    })
  );

  // User cannot self-activate paid subscription
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      subscriptionStatus: "ACTIVE",
      updatedAt: pastDate(1000),
    })
  );

  // User can start one-time 7-day free trial when NOT_STARTED
  const trialStart = pastDate(1000);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      trialStatus: "ACTIVE",
      trialStartAt: trialStart,
      trialExpiresAt: futureDate(),
      trialActivatedMethod: "USER_BUTTON",
      subscriptionStatus: "TRIAL",
      updatedAt: pastDate(500),
    })
  );

  // User can update profile name and preferredLanguage (UR/AR/EN)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      fullName: "Alice Architecture",
      preferredLanguage: "UR",
      updatedAt: pastDate(200),
    })
  );

  // If trial becomes EXPIRED, user cannot restart trial again
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).update({
      trialStatus: "EXPIRED",
      subscriptionStatus: "EXPIRED",
    });
  });

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      trialStatus: "ACTIVE",
      trialStartAt: pastDate(500),
      trialExpiresAt: futureDate(),
      trialActivatedMethod: "USER_BUTTON",
      subscriptionStatus: "TRIAL",
      updatedAt: pastDate(100),
    })
  );
});

test("Cross-user isolation on users, favorites, downloads, notifications, and courseProgress", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = pastDate(5000);
    await db.collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      fullName: "Bob",
      email: "bob@example.com",
      role: "USER",
      accountStatus: "ACTIVE",
      preferredLanguage: "EN",
      trialStatus: "NOT_STARTED",
      subscriptionStatus: "NONE",
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("favorites").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("downloads").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("notifications").get());

  // Alice can write her own favorite & download log
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("favorites").doc("res_1").set({
      userId: ALICE_UID,
      resourceId: "res_1",
      title: "5 Marla House Map",
      categoryId: "NAQSHAY",
      fileType: "DXF",
      createdAt: pastDate(500),
    })
  );
});

test("Email spoofing test: unverified admin email is rejected", async () => {
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(
    spoofedAdminDb.collection("resources").doc("res_1").set({
      title: "Fake Resource",
      description: "Desc",
      categoryId: "PDF_LESSONS",
      fileUrl: "asset://academy/sample.pdf",
      fileName: "sample.pdf",
      fileType: "PDF",
      fileSizeLabel: "1.2 MB",
      fileSizeBytes: 1200000,
      premium: true,
      published: true,
      archived: false,
      uploadedBy: "spoof_uid",
      createdAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );
});

test("Super Admin resource publishing, filtered catalog queries, and JazzCash/Easypaisa payment verification", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  const now = pastDate(5000);

  // Admin creates subscription plan
  await assertSucceeds(
    adminDb.collection("subscriptionPlans").doc("plan_monthly").set({
      title: "Monthly Pro",
      description: "Full access to all CAD, DXF, DWG, 3D Max, and Courses",
      pricePkr: 1500,
      durationDays: 30,
      accessLevel: "ALL_ACCESS",
      available: true,
      badgeText: "Popular",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin publishes resource
  await assertSucceeds(
    adminDb.collection("resources").doc("res_dwg_1").set({
      title: "AutoCAD Floor Plan DWG File",
      description: "Complete 10 Marla residential floor plan with electrical & plumbing layers.",
      categoryId: "DWG",
      fileUrl: "asset://academy/autocad_floor_plan.dwg",
      fileName: "autocad_floor_plan.dwg",
      fileType: "DWG",
      fileSizeLabel: "2.4 MB",
      fileSizeBytes: 2450000,
      premium: true,
      published: true,
      archived: false,
      uploadedBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Normal user cannot publish resource
  await assertFails(
    aliceDb.collection("resources").doc("res_unauth").set({
      title: "Unauthorized",
      description: "No",
      categoryId: "DWG",
      fileUrl: "asset://academy/x.dwg",
      fileName: "x.dwg",
      fileType: "DWG",
      fileSizeLabel: "1 MB",
      fileSizeBytes: 1000,
      premium: false,
      published: true,
      archived: false,
      uploadedBy: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Normal user filtered query succeeds; unfiltered query fails if unpublished docs exist
  await assertSucceeds(
    aliceDb.collection("resources").where("published", "==", true).where("archived", "==", false).get()
  );
  await assertSucceeds(
    aliceDb.collection("subscriptionPlans").where("available", "==", true).get()
  );

  // Alice submits JazzCash payment request
  await assertSucceeds(
    aliceDb.collection("paymentRequests").doc("pay_req_1").set({
      userId: ALICE_UID,
      userEmail: "alice@example.com",
      userName: "Alice Khan",
      planId: "plan_monthly",
      planTitle: "Monthly Pro",
      provider: "JAZZCASH",
      amountPkr: 1500,
      transactionReference: "TXN987654321",
      proofLabel: "receipt_jazzcash.jpg",
      status: "PENDING",
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );

  // Alice CANNOT approve her own payment request
  await assertFails(
    aliceDb.collection("paymentRequests").doc("pay_req_1").update({
      status: "APPROVED",
      updatedAt: pastDate(500),
    })
  );

  // Alice CANNOT inject @DocumentId "id" field
  await assertFails(
    aliceDb.collection("paymentRequests").doc("pay_req_2").set({
      id: "pay_req_2",
      userId: ALICE_UID,
      userEmail: "alice@example.com",
      userName: "Alice Khan",
      planId: "plan_monthly",
      planTitle: "Monthly Pro",
      provider: "EASYPAISA",
      amountPkr: 1500,
      transactionReference: "EP123456",
      status: "PENDING",
      createdAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );

  // Seed a payment request owned by Bob to verify list rule isolation
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("paymentRequests").doc("bob_pay_req").set({
      userId: BOB_UID,
      userEmail: "bob@example.com",
      userName: "Bob",
      planId: "plan_monthly",
      planTitle: "Monthly Pro",
      provider: "EASYPAISA",
      amountPkr: 1500,
      transactionReference: "BOB999999",
      status: "PENDING",
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    });
  });

  // Alice queries her own paymentRequests
  await assertSucceeds(
    aliceDb.collection("paymentRequests").where("userId", "==", ALICE_UID).get()
  );
  // Alice unfiltered query fails because Bob's doc is in the collection
  await assertFails(aliceDb.collection("paymentRequests").get());

  // Admin approves Alice's payment request
  await assertSucceeds(
    adminDb.collection("paymentRequests").doc("pay_req_1").update({
      status: "APPROVED",
      verificationNote: "Verified JazzCash TID",
      reviewedBy: ADMIN_UID,
      reviewedAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );
});
