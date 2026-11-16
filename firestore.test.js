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

test("outsider cannot read or write depot data, enumerate non-existent staff_links, or shadow-update fields", async () => {
  await seedDepotWithStaff();
  const oDb = outsiderDb();
  await assertFails(oDb.collection("depots").doc(DEPOT_ID).get());
  await assertFails(oDb.collection("staff_links").doc(STAFF_EMAIL).get());
  await assertFails(oDb.collection("staff_links").doc("nonexistent@santoya.test").get());
  await assertFails(oDb.collection("depots").doc(DEPOT_ID).collection("pelanggan").get());

  const oDbOwner = ownerDb();
  await assertSucceeds(
    oDbOwner.collection("depots").doc(DEPOT_ID).collection("transaksi").doc("tx_v16").set({
      syncId: "tx_v16",
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
      tanggalBayar: 1700000050000,
      deletedBy: null,
      _hardDeleted: false,
      updatedAt: 1700000050000,
      isDeleted: false,
      serverUpdatedAt: ts(),
    })
  );

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

test("revoking staff removes access immediately and real owner can reclaim hijacked staff_link", async () => {
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

  // Simulate another account (outsider acting as fake owner) having created a staff_link for STAFF_EMAIL
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("staff_links").doc(STAFF_EMAIL).set({
      depotId: `depot_${OUTSIDER_UID}`,
      ownerUid: OUTSIDER_UID,
      ownerEmail: OUTSIDER_EMAIL,
      staffEmail: STAFF_EMAIL,
      active: true,
      createdAt: ts(),
      updatedAt: ts(),
    });
  });

  // Real owner adds STAFF_EMAIL back to their depot memberEmails and reclaims staff_links/STAFF_EMAIL
  await assertSucceeds(
    oDb.collection("depots").doc(DEPOT_ID).update({
      memberEmails: [STAFF_EMAIL],
      updatedAt: ts(),
    })
  );
  await assertSucceeds(
    oDb.collection("staff_links").doc(STAFF_EMAIL).set({
      depotId: DEPOT_ID,
      ownerUid: OWNER_UID,
      ownerEmail: OWNER_EMAIL,
      staffEmail: STAFF_EMAIL,
      active: true,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
});

test("owner can CRUD perawatan_jadwal and perawatan_log while staff, outsider, and unauthenticated users are denied", async () => {
  await seedDepotWithStaff();
  const oDb = ownerDb();
  const sDb = staffDb(true);
  const outDb = outsiderDb();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  const jadwalRef = oDb.collection("depots").doc(DEPOT_ID).collection("perawatan_jadwal").doc("pw_tds");
  const logRef = oDb.collection("depots").doc(DEPOT_ID).collection("perawatan_log").doc("pwlog_1");

  // Owner can create, read, update, and list perawatan_jadwal & perawatan_log
  await assertSucceeds(
    jadwalRef.set({
      syncId: "pw_tds",
      nama: "Cek TDS",
      intervalHari: 7,
      terakhirSelesai: null,
      jadwalBerikutnya: 1700600000000,
      updatedAt: 1700000000000,
      isDeleted: false,
      serverUpdatedAt: ts(),
    })
  );
  await assertSucceeds(jadwalRef.get());
  await assertSucceeds(oDb.collection("depots").doc(DEPOT_ID).collection("perawatan_jadwal").get());

  await assertSucceeds(
    logRef.set({
      syncId: "pwlog_1",
      perawatanSyncId: "pw_tds",
      namaPerawatan: "Cek TDS",
      tanggalSelesai: 1700000000000,
      biaya: 50000,
      catatan: "Air jernih 14 ppm",
      pengeluaranSyncId: "exp_pw_1",
      updatedAt: 1700000000000,
      isDeleted: false,
      serverUpdatedAt: ts(),
    })
  );
  await assertSucceeds(logRef.get());
  await assertSucceeds(oDb.collection("depots").doc(DEPOT_ID).collection("perawatan_log").get());

  // Schema validation rejects invalid intervalHari / negative biaya / unexpected field
  await assertFails(
    oDb.collection("depots").doc(DEPOT_ID).collection("perawatan_jadwal").doc("pw_bad").set({
      syncId: "pw_bad",
      nama: "Invalid Interval",
      intervalHari: 0,
      terakhirSelesai: null,
      jadwalBerikutnya: 1700600000000,
      updatedAt: 1700000000000,
      isDeleted: false,
      serverUpdatedAt: ts(),
    })
  );
  await assertFails(
    oDb.collection("depots").doc(DEPOT_ID).collection("perawatan_log").doc("pwlog_bad").set({
      syncId: "pwlog_bad",
      perawatanSyncId: "pw_tds",
      namaPerawatan: "Cek TDS",
      tanggalSelesai: 1700000000000,
      biaya: -100,
      catatan: "",
      pengeluaranSyncId: null,
      updatedAt: 1700000000000,
      isDeleted: false,
      serverUpdatedAt: ts(),
    })
  );

  // Staff cannot get, list, create, update, or delete perawatan_jadwal or perawatan_log
  await assertFails(sDb.collection("depots").doc(DEPOT_ID).collection("perawatan_jadwal").doc("pw_tds").get());
  await assertFails(sDb.collection("depots").doc(DEPOT_ID).collection("perawatan_jadwal").get());
  await assertFails(
    sDb.collection("depots").doc(DEPOT_ID).collection("perawatan_jadwal").doc("pw_staff").set({
      syncId: "pw_staff",
      nama: "Bypass",
      intervalHari: 7,
      terakhirSelesai: null,
      jadwalBerikutnya: 1700600000000,
      updatedAt: 1700000000000,
      isDeleted: false,
      serverUpdatedAt: ts(),
    })
  );
  await assertFails(sDb.collection("depots").doc(DEPOT_ID).collection("perawatan_jadwal").doc("pw_tds").update({ nama: "Hacked", updatedAt: 1700000000001, serverUpdatedAt: ts() }));
  await assertFails(sDb.collection("depots").doc(DEPOT_ID).collection("perawatan_jadwal").doc("pw_tds").delete());

  await assertFails(sDb.collection("depots").doc(DEPOT_ID).collection("perawatan_log").doc("pwlog_1").get());
  await assertFails(sDb.collection("depots").doc(DEPOT_ID).collection("perawatan_log").get());
  await assertFails(
    sDb.collection("depots").doc(DEPOT_ID).collection("perawatan_log").doc("pwlog_staff").set({
      syncId: "pwlog_staff",
      perawatanSyncId: "pw_tds",
      namaPerawatan: "Cek TDS",
      tanggalSelesai: 1700000000000,
      biaya: 0,
      catatan: "Staff bypass",
      pengeluaranSyncId: null,
      updatedAt: 1700000000000,
      isDeleted: false,
      serverUpdatedAt: ts(),
    })
  );
  await assertFails(sDb.collection("depots").doc(DEPOT_ID).collection("perawatan_log").doc("pwlog_1").delete());

  // Outsider & unauthenticated denied
  await assertFails(outDb.collection("depots").doc(DEPOT_ID).collection("perawatan_jadwal").get());
  await assertFails(outDb.collection("depots").doc(DEPOT_ID).collection("perawatan_log").get());
  await assertFails(unauthDb.collection("depots").doc(DEPOT_ID).collection("perawatan_jadwal").get());
  await assertFails(unauthDb.collection("depots").doc(DEPOT_ID).collection("perawatan_log").get());

  // Owner can delete own perawatan docs
  await assertSucceeds(logRef.delete());
  await assertSucceeds(jadwalRef.delete());
});

