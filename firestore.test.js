const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const ts = () => new Date(Date.now() - 2000);

const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const OWNER_UID = "owner_uid_1";
const OWNER_EMAIL = "owner@santoya.test";
const STAFF_UID = "staff_uid_2";
const STAFF_EMAIL = "staff@santoya.test";
const OUTSIDER_UID = "outsider_uid_3";
const OUTSIDER_EMAIL = "outsider@santoya.test";
const DEPOT_ID = "depot_owner_uid_1";

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: emulatorHost,
      port: emulatorPort,
      rules: fs.readFileSync("firestore.rules", "utf8"),
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

function ownerDb() {
  return testEnv.authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: true }).firestore();
}

function staffDb(verified = true) {
  return testEnv.authenticatedContext(STAFF_UID, { email: STAFF_EMAIL, email_verified: verified }).firestore();
}

function outsiderDb() {
  return testEnv.authenticatedContext(OUTSIDER_UID, { email: OUTSIDER_EMAIL, email_verified: true }).firestore();
}

async function seedDepotWithStaff() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("depots").doc(DEPOT_ID).set({
      depotId: DEPOT_ID,
      ownerUid: OWNER_UID,
      ownerEmail: OWNER_EMAIL,
      name: "Santoya RO",
      memberEmails: [STAFF_EMAIL],
      createdAt: ts(),
      updatedAt: ts(),
    });
    await db.collection("staff_links").doc(STAFF_EMAIL).set({
      depotId: DEPOT_ID,
      ownerUid: OWNER_UID,
      ownerEmail: OWNER_EMAIL,
      staffEmail: STAFF_EMAIL,
      active: true,
      createdAt: ts(),
      updatedAt: ts(),
    });
  });
}

test("unauthenticated user cannot access depots, staff_links, or transactions", async () => {
  await seedDepotWithStaff();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("depots").doc(DEPOT_ID).get());
  await assertFails(unauthDb.collection("staff_links").doc(STAFF_EMAIL).get());
  await assertFails(unauthDb.collection("depots").doc(DEPOT_ID).collection("transaksi").get());
});

test("owner can read non-existent depot/staff_link on first login, create depot, register staff_link, and manage produk & pengeluaran", async () => {
  const db = ownerDb();
  // First-time login: documents do not exist yet (resource == null)
  await assertSucceeds(db.collection("staff_links").doc(OWNER_EMAIL).get());
  await assertSucceeds(db.collection("depots").doc(DEPOT_ID).get());
  await assertSucceeds(db.collection("depots").doc(DEPOT_ID).collection("produk").get());

  await assertSucceeds(
    db.collection("depots").doc(DEPOT_ID).set({
      depotId: DEPOT_ID,
      ownerUid: OWNER_UID,
      ownerEmail: OWNER_EMAIL,
      name: "Santoya RO",
      memberEmails: [STAFF_EMAIL],
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    db.collection("staff_links").doc(STAFF_EMAIL).set({
      depotId: DEPOT_ID,
      ownerUid: OWNER_UID,
      ownerEmail: OWNER_EMAIL,
      staffEmail: STAFF_EMAIL,
      active: true,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    db.collection("depots").doc(DEPOT_ID).collection("produk").doc("prod_1").set({
      syncId: "prod_1",
      nama_produk: "Galon RO 19L",
      harga: 6000,
      updatedAt: 1700000000000,
      isDeleted: false,
      serverUpdatedAt: ts(),
    })
  );

  await assertSucceeds(
    db.collection("depots").doc(DEPOT_ID).collection("pengeluaran").doc("exp_1").set({
      syncId: "exp_1",
      tanggal: 1700000000000,
      nama_item: "Filter Sedimen",
      kategori: "Perawatan",
      nominal: 50000,
      updatedAt: 1700000000000,
      isDeleted: false,
      serverUpdatedAt: ts(),
    })
  );
});

test("verified active staff can read depot/staff_link and write transaksi/pelanggan, but CANNOT access pengeluaran or modify produk/kurir", async () => {
  await seedDepotWithStaff();
  const sDb = staffDb(true);

  await assertSucceeds(sDb.collection("staff_links").doc(STAFF_EMAIL).get());
  await assertSucceeds(sDb.collection("depots").doc(DEPOT_ID).get());
  await assertSucceeds(sDb.collection("depots").doc(DEPOT_ID).collection("produk").get());
  await assertSucceeds(sDb.collection("depots").doc(DEPOT_ID).collection("kurir").get());

  await assertSucceeds(
    sDb.collection("depots").doc(DEPOT_ID).collection("pelanggan").doc("cust_1").set({
      syncId: "cust_1",
      nama: "Pak Andi",
      alamat: "Jl. Kenanga",
      no_hp: "08123456",
      sisa_kasbon: 0,
      updatedAt: 1700000000000,
      isDeleted: false,
      serverUpdatedAt: ts(),
    })
  );

  await assertSucceeds(
    sDb.collection("depots").doc(DEPOT_ID).collection("transaksi").doc("tx_1").set({
      syncId: "tx_1",
      tanggal: 1700000000000,
      pelangganSyncId: "cust_1",
      jumlah: 2,
      harga_satuan: 6000,
      total_harga: 12000,
      total_komisi: 2000,
      metode_bayar: "Cash",
      status: "Lunas",
      uangMuka: 0,
      sisaKasbon: 0,
      updatedAt: 1700000000000,
      isDeleted: false,
      serverUpdatedAt: ts(),
    })
  );

  await assertFails(sDb.collection("depots").doc(DEPOT_ID).collection("pengeluaran").get());
  await assertFails(
    sDb.collection("depots").doc(DEPOT_ID).collection("pengeluaran").doc("exp_staff").set({
      syncId: "exp_staff",
      tanggal: 1700000000000,
      nama_item: "Hack",
      kategori: "Operasional",
      nominal: 1000,
      updatedAt: 1700000000000,
      isDeleted: false,
      serverUpdatedAt: ts(),
    })
  );
  await assertFails(
    sDb.collection("depots").doc(DEPOT_ID).collection("produk").doc("prod_staff").set({
      syncId: "prod_staff",
      nama_produk: "Gratis",
      harga: 0,
      updatedAt: 1700000000000,
      isDeleted: false,
      serverUpdatedAt: ts(),
    })
  );
  await assertFails(
    sDb.collection("depots").doc(DEPOT_ID).collection("kurir").doc("kur_staff").set({
      syncId: "kur_staff",
      nama: "Self",
      komisi_per_galon: 99999,
      email: STAFF_EMAIL,
      akses_aktif: true,
      skema_komisi: "TETAP",
      aturan_komisi: "",
      updatedAt: 1700000000000,
      isDeleted: false,
      serverUpdatedAt: ts(),
    })
  );
});

test("unverified staff email is rejected (email spoofing protection)", async () => {
  await seedDepotWithStaff();
  const unverifiedStaffDb = staffDb(false);
  await assertFails(unverifiedStaffDb.collection("staff_links").doc(STAFF_EMAIL).get());
  await assertFails(unverifiedStaffDb.collection("depots").doc(DEPOT_ID).collection("transaksi").get());
});

test("outsider cannot read or write depot data or shadow-update fields", async () => {
  await seedDepotWithStaff();
  const oDb = outsiderDb();
  await assertFails(oDb.collection("depots").doc(DEPOT_ID).get());
  await assertFails(oDb.collection("staff_links").doc(STAFF_EMAIL).get());
  await assertFails(oDb.collection("depots").doc(DEPOT_ID).collection("pelanggan").get());

  const oDbOwner = ownerDb();
  await assertFails(
    oDbOwner.collection("depots").doc(DEPOT_ID).collection("produk").doc("prod_bad").set({
      syncId: "prod_bad",
      nama_produk: "Galon",
      harga: 6000,
      updatedAt: 1700000000000,
      isDeleted: false,
      serverUpdatedAt: ts(),
      ghostField: "hack",
    })
  );
});

test("revoking staff removes access immediately", async () => {
  await seedDepotWithStaff();
  const oDb = ownerDb();
  await assertSucceeds(
    oDb.collection("depots").doc(DEPOT_ID).update({
      memberEmails: [],
      updatedAt: ts(),
    })
  );
  const sDb = staffDb(true);
  await assertFails(sDb.collection("depots").doc(DEPOT_ID).collection("transaksi").get());
});
