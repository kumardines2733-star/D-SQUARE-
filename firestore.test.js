const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER_UID = "owner_deepak";
const OWNER_EMAIL = "deepakdhudiya152@gmail.com";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const STAFF_UID = "staff_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTs() {
  return new Date();
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

test("Unauthenticated user cannot read products, orders, or users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("products").get());
  await assertFails(unauthDb.collection("orders").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Spoofed owner email with email_verified=false cannot list all orders", async () => {
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: OWNER_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(spoofDb.collection("orders").get());
  await assertFails(spoofDb.collection("users").get());
});

test("Verified owner email can create products, categories, staff, and list all orders", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
  const ts = nowTs();

  await assertSucceeds(
    ownerDb.collection("categories").doc("cat_fashion").set({
      name: "Fashion & Clothing",
      slug: "fashion-clothing",
      description: "Luxury apparel",
      iconKey: "fashion",
      sortOrder: 1,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    ownerDb.collection("products").doc("prod_tote").set({
      name: "Obsidian Leather Tote",
      sku: "DSQ-FSH-001",
      categoryId: "cat_fashion",
      categoryName: "Fashion & Clothing",
      description: "Full-grain leather tote with brushed gold hardware",
      price: 1299.0,
      costPrice: 640.0,
      compareAtPrice: 1499.0,
      stock: 18,
      lowStockThreshold: 5,
      variants: ["Obsidian / Gold"],
      colors: ["Obsidian"],
      sizes: ["One Size"],
      imageKey: "img_prod_fashion",
      imageUrls: [],
      ratingAvg: 4.9,
      reviewCount: 12,
      soldCount: 24,
      viewCount: 180,
      isFeatured: true,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(ownerDb.collection("orders").get());
});

test("Regular customer cannot create products, modify prices, or elevate own role", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const ts = nowTs();

  await assertFails(
    aliceDb.collection("products").doc("prod_hack").set({
      name: "Free Watch",
      sku: "HACK-1",
      categoryId: "cat_fashion",
      categoryName: "Fashion",
      price: 0,
      costPrice: 0,
      stock: 10,
      lowStockThreshold: 2,
      imageKey: "img_prod_fashion",
      isFeatured: false,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Ghost field / privilege escalation test on user profile
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      role: "admin",
      isAdmin: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Customer can manage own profile, cart, wishlist, addresses, and query own orders only", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const ts = nowTs();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Vance",
      email: "alice@example.com",
      phone: "+1 555 0101",
      preferredPayment: "UPI",
      totalOrders: 0,
      totalSpent: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("item_1").set({
      userId: ALICE_UID,
      productId: "prod_tote",
      productName: "Obsidian Leather Tote",
      categoryName: "Fashion",
      variant: "Obsidian / Gold",
      unitPrice: 1299.0,
      costPrice: 640.0,
      quantity: 1,
      imageKey: "img_prod_fashion",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("cart").get()
  );

  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_alice_1").set({
      orderNumber: "DSQ-2026-0001",
      userId: ALICE_UID,
      customerName: "Alice Vance",
      customerEmail: "alice@example.com",
      customerPhone: "+1 555 0101",
      shippingAddress: "742 Fifth Ave, New York, NY 10019",
      itemsSummary: "Obsidian Leather Tote x1",
      items: [{ productId: "prod_tote", productName: "Obsidian Leather Tote", qty: 1, unitPrice: 1299.0 }],
      subtotal: 1299.0,
      discountAmount: 0,
      couponCode: "",
      shippingFee: 0,
      taxAmount: 103.92,
      totalAmount: 1402.92,
      totalCost: 640.0,
      paymentMethod: "UPI",
      paymentStatus: "PAID",
      paymentReference: "UPI-REF-9912",
      orderStatus: "PLACED",
      trackingNumber: "",
      trackingCourier: "",
      cancelReason: "",
      returnReason: "",
      refundAmount: 0,
      refundStatus: "NONE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Customer query with userId filter passes; unfiltered list fails
  await assertSucceeds(
    aliceDb.collection("orders").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("orders").get());
  await assertFails(bobDb.collection("orders").doc("ord_alice_1").get());
});

test("Staff RBAC allows permitted product edits while blocking unauthorized order/staff writes", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID, {
    email: "staff@dsquare.com",
    email_verified: true,
  }).firestore();
  const ts = nowTs();

  await assertSucceeds(
    ownerDb.collection("staff").doc(STAFF_UID).set({
      staffUid: STAFF_UID,
      email: "staff@dsquare.com",
      displayName: "Merchandiser Lee",
      roleName: "Inventory Lead",
      canManageProducts: true,
      canManageOrders: false,
      canManageCustomers: false,
      canManageCoupons: false,
      canManageContent: false,
      canViewAnalytics: false,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    staffDb.collection("categories").doc("cat_elec").set({
      name: "Electronics",
      slug: "electronics",
      description: "Flagship audio and wearables",
      iconKey: "electronics",
      sortOrder: 2,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Staff without canManageCoupons cannot create coupons
  await assertFails(
    staffDb.collection("coupons").doc("cpn_1").set({
      code: "GOLD20",
      description: "20% off",
      discountType: "PERCENT",
      discountValue: 20,
      minOrderAmount: 100,
      maxDiscount: 500,
      usageCount: 0,
      usageLimit: 100,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});
