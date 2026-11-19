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

test("transaksi enforces isValidTransaksiMath on total_harga, uangMuka, sisaKasbon, komisi, jumlah, and harga_satuan", async () => {
  await seedDepotWithStaff();
  const oDb = ownerDb();
  const txCol = oDb.collection("depots").doc(DEPOT_ID).collection("transaksi");

  const validBase = (id, overrides = {}) => ({
    syncId: id,
    tanggal: 1700000000000,
    pelangganSyncId: "cust_1",
    jumlah: 3,
    harga_satuan: 6000,
    total_harga: 18000,
    total_komisi: 3000,
    metode_bayar: "Cash",
    status: "Lunas",
    uangMuka: 0,
    sisaKasbon: 0,
    updatedAt: 1700000000000,
    isDeleted: false,
    serverUpdatedAt: ts(),
    ...overrides,
  });

  // Valid Lunas, Kasbon penuh, Bayar Sebagian, and soft-deleted Dihapus
  await assertSucceeds(txCol.doc("tx_math_lunas").set(validBase("tx_math_lunas")));
  await assertSucceeds(
    txCol.doc("tx_math_kasbon").set(
      validBase("tx_math_kasbon", {
        metode_bayar: "-",
        status: "Kasbon",
        uangMuka: 0,
        sisaKasbon: 18000,
      })
    )
  );
  await assertSucceeds(
    txCol.doc("tx_math_dp").set(
      validBase("tx_math_dp", {
        metode_bayar: "Bayar Sebagian",
        status: "Kasbon",
        uangMuka: 5000,
        sisaKasbon: 13000,
      })
    )
  );
  await assertSucceeds(
    txCol.doc("tx_math_dihapus").set(
      validBase("tx_math_dihapus", {
        status: "Dihapus",
        isDeleted: true,
        total_komisi: 0,
      })
    )
  );

  // Manipulated total_harga (jumlah * harga_satuan mismatch)
  await assertFails(
    txCol.doc("tx_bad_total").set(
      validBase("tx_bad_total", {
        total_harga: 1000,
      })
    )
  );

  // uangMuka exceeds total_harga
  await assertFails(
    txCol.doc("tx_bad_dp").set(
      validBase("tx_bad_dp", {
        status: "Kasbon",
        uangMuka: 20000,
        sisaKasbon: 0,
      })
    )
  );

  // Kasbon sisaKasbon does not match total_harga - uangMuka
  await assertFails(
    txCol.doc("tx_bad_kasbon_sisa").set(
      validBase("tx_bad_kasbon_sisa", {
        status: "Kasbon",
        metode_bayar: "Bayar Sebagian",
        uangMuka: 5000,
        sisaKasbon: 1000,
      })
    )
  );

  // Lunas with non-zero sisaKasbon
  await assertFails(
    txCol.doc("tx_bad_lunas_sisa").set(
      validBase("tx_bad_lunas_sisa", {
        status: "Lunas",
        sisaKasbon: 5000,
      })
    )
  );

  // Negative komisi, invalid jumlah, negative harga_satuan
  await assertFails(txCol.doc("tx_bad_komisi").set(validBase("tx_bad_komisi", { total_komisi: -100 })));
  await assertFails(txCol.doc("tx_bad_jumlah").set(validBase("tx_bad_jumlah", { jumlah: 0, total_harga: 0 })));
  await assertFails(txCol.doc("tx_bad_harga").set(validBase("tx_bad_harga", { harga_satuan: -1000, total_harga: -3000 })));
});

test("kasbon_logs enforces isValidKasbonLogMath on jumlah_bayar and sisa_kasbon balances", async () => {
  await seedDepotWithStaff();
  const sDb = staffDb(true);
  const logsCol = sDb.collection("depots").doc(DEPOT_ID).collection("kasbon_logs");

  const validLog = (id, overrides = {}) => ({
    syncId: id,
    pelangganSyncId: "cust_1",
    tanggal: 1700000000000,
    jumlah_bayar: 10000,
    sisa_kasbon_sebelum: 18000,
    sisa_kasbon_sesudah: 8000,
    metode_bayar: "Cash",
    catatan: "Cicilan kasbon",
    updatedAt: 1700000000000,
    isDeleted: false,
    serverUpdatedAt: ts(),
    ...overrides,
  });

  await assertSucceeds(logsCol.doc("log_valid").set(validLog("log_valid")));

  // Negative jumlah_bayar
  await assertFails(
    logsCol.doc("log_neg_bayar").set(
      validLog("log_neg_bayar", {
        jumlah_bayar: -5000,
        sisa_kasbon_sebelum: 10000,
        sisa_kasbon_sesudah: 15000,
      })
    )
  );

  // Negative sisa_kasbon_sebelum
  await assertFails(
    logsCol.doc("log_neg_sebelum").set(
      validLog("log_neg_sebelum", {
        jumlah_bayar: 0,
        sisa_kasbon_sebelum: -1000,
        sisa_kasbon_sesudah: -1000,
      })
    )
  );

  // Mismatched sisa_kasbon_sesudah != sisa_kasbon_sebelum - jumlah_bayar
  await assertFails(
    logsCol.doc("log_mismatch_sesudah").set(
      validLog("log_mismatch_sesudah", {
        jumlah_bayar: 10000,
        sisa_kasbon_sebelum: 18000,
        sisa_kasbon_sesudah: 0,
      })
    )
  );

  // Negative sisa_kasbon_sesudah (jumlah_bayar > sisa_kasbon_sebelum)
  await assertFails(
    logsCol.doc("log_neg_sesudah").set(
      validLog("log_neg_sesudah", {
        jumlah_bayar: 20000,
        sisa_kasbon_sebelum: 18000,
        sisa_kasbon_sesudah: -2000,
      })
    )
  );
});

test("users/{uid} allows owner self-registration & merge updates while locking registeredAt and subscriptionTier", async () => {
  const oDb = ownerDb();
  const sDb = staffDb(true);
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const createdAt = new Date(Date.now() - 10000);

  const validInitialUser = {
    uid: OWNER_UID,
    email: OWNER_EMAIL,
    googleDisplayName: "Budi Santoso",
    ownerName: "Budi Santoso",
    depotName: "",
    phoneNumber: "",
    address: "",
    receiptFooter: "",
    role: "OWNER",
    subscriptionTier: "FREE",
    registeredAt: createdAt,
    lastActiveAt: createdAt,
    appVersion: "1.3",
  };

  // Unauthenticated or cross-user read/write must fail
  await assertFails(unauthDb.collection("users").doc(OWNER_UID).set(validInitialUser));
  await assertFails(sDb.collection("users").doc(OWNER_UID).set(validInitialUser));
  await assertFails(oDb.collection("users").get());

  // Initial create with PRO tier from client must fail
  await assertFails(
    oDb.collection("users").doc(OWNER_UID).set({
      ...validInitialUser,
      subscriptionTier: "PRO",
    })
  );

  // Valid initial create succeeds
  await assertSucceeds(oDb.collection("users").doc(OWNER_UID).set(validInitialUser));
  await assertSucceeds(oDb.collection("users").doc(OWNER_UID).get());

  // Cross-user read fails
  await assertFails(sDb.collection("users").doc(OWNER_UID).get());

  // Valid profile merge update without modifying registeredAt or subscriptionTier succeeds
  await assertSucceeds(
    oDb.collection("users").doc(OWNER_UID).set(
      {
        depotName: "Berkah RO",
        phoneNumber: "081234567890",
        address: "Jl. Merdeka No. 12, Malang",
        receiptFooter: "Terima kasih sudah berlangganan",
        lastActiveAt: ts(),
        appVersion: "1.3",
      },
      { merge: true }
    )
  );

  // Tampering with subscriptionTier or registeredAt on update must fail
  await assertFails(
    oDb.collection("users").doc(OWNER_UID).set(
      {
        subscriptionTier: "PRO",
        lastActiveAt: ts(),
      },
      { merge: true }
    )
  );
  await assertFails(
    oDb.collection("users").doc(OWNER_UID).set(
      {
        registeredAt: ts(),
        lastActiveAt: ts(),
      },
      { merge: true }
    )
  );
});

test("depots/{depotId} allows owner to sync optional profile fields and verified staff to read them", async () => {
  await seedDepotWithStaff();
  const oDb = ownerDb();
  const sDb = staffDb(true);

  await assertSucceeds(
    oDb.collection("depots").doc(DEPOT_ID).set(
      {
        name: "Berkah RO",
        depotName: "Berkah RO",
        ownerName: "Budi Santoso",
        phoneNumber: "081234567890",
        address: "Jl. Merdeka No. 12, Malang",
        receiptFooter: "Segar setiap hari",
        subscriptionTier: "FREE",
        updatedAt: ts(),
      },
      { merge: true }
    )
  );

  await assertSucceeds(sDb.collection("depots").doc(DEPOT_ID).get());

  // Staff cannot overwrite depot profile metadata
  await assertFails(
    sDb.collection("depots").doc(DEPOT_ID).set(
      {
        depotName: "Dibajak Staff",
        updatedAt: ts(),
      },
      { merge: true }
    )
  );
});

test("ai_insights/{insightId} enforces strict owner-only isolation, schema bounds, and filtered queries", async () => {
  const oDb = ownerDb();
  const sDb = staffDb(true);
  const outDb = outsiderDb();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  const insightDocId = `latest_${OWNER_UID}`;
  const validInsightPayload = (overrides = {}) => ({
    ownerId: OWNER_UID,
    depotId: DEPOT_ID,
    periodDays: 30,
    insights: [
      "Penjualan galon RO mendominasi 78% omzet 30 hari terakhir.",
      "Pelanggan aktif meningkat dan rasio lunas tetap terjaga baik.",
      "Pengeluaran operasional utama berasal dari biaya filter & listrik.",
    ],
    stockAdvice: "Stok air RO toren masih cukup untuk sekitar 3 hari, jadwalkan pengisian sebelum akhir pekan.",
    healthStatus: "SEHAT",
    priorityActionTitle: "Tagih Kasbon >30 Hari",
    priorityActionTarget: "KASBON",
    totalOmzet: 1250000,
    totalKasMasuk: 1180000,
    totalKomisiKurir: 150000,
    totalPengeluaran: 220000,
    labaBersih: 810000,
    marginLabaPersen: 68.6,
    totalGalon: 210,
    activeCustomers: 45,
    totalPiutang: 95000,
    overduePiutangOver30d: 35000,
    omzetGrowthPercent: 12.5,
    galonGrowthPercent: 8.0,
    labaGrowthPercent: 15.2,
    modelName: "gemini-3.8-flash",
    timestamp: ts(),
    ...overrides,
  });

  // 1. Owner can attach listener/get non-existent latest_{uid} before first analysis
  await assertSucceeds(oDb.collection("ai_insights").doc(insightDocId).get());

  // 2. Unauthenticated, staff, and outsider cannot read non-existent or other user's doc
  await assertFails(unauthDb.collection("ai_insights").doc(insightDocId).get());
  await assertFails(sDb.collection("ai_insights").doc(insightDocId).get());
  await assertFails(outDb.collection("ai_insights").doc(insightDocId).get());
  await assertFails(unauthDb.collection("ai_insights").where("ownerId", "==", OWNER_UID).get());

  // 3. Unfiltered collection list is denied even for owner
  await assertFails(oDb.collection("ai_insights").get());

  // 4. Schema & ownership validation on create
  await assertFails(
    oDb.collection("ai_insights").doc("custom_doc_id").set(validInsightPayload())
  );
  await assertFails(
    oDb.collection("ai_insights").doc(insightDocId).set(validInsightPayload({ ownerId: OUTSIDER_UID }))
  );
  await assertFails(
    oDb.collection("ai_insights").doc(insightDocId).set(validInsightPayload({ depotId: `depot_${OUTSIDER_UID}` }))
  );
  await assertFails(
    oDb.collection("ai_insights").doc(insightDocId).set(validInsightPayload({ insights: [] }))
  );
  await assertFails(
    oDb.collection("ai_insights").doc(insightDocId).set(validInsightPayload({ stockAdvice: "" }))
  );
  await assertFails(
    oDb.collection("ai_insights").doc(insightDocId).set(validInsightPayload({ stockAdvice: "x".repeat(1001) }))
  );
  await assertFails(
    oDb.collection("ai_insights").doc(insightDocId).set(validInsightPayload({ totalOmzet: -1 }))
  );
  await assertFails(
    oDb.collection("ai_insights").doc(insightDocId).set(validInsightPayload({ totalKasMasuk: -10 }))
  );
  await assertFails(
    oDb.collection("ai_insights").doc(insightDocId).set(validInsightPayload({ overduePiutangOver30d: -5 }))
  );
  await assertFails(
    oDb.collection("ai_insights").doc(insightDocId).set(validInsightPayload({ periodDays: 14 }))
  );
  await assertFails(
    oDb.collection("ai_insights").doc(insightDocId).set(validInsightPayload({ healthStatus: "BURUK" }))
  );
  await assertFails(
    oDb.collection("ai_insights").doc(insightDocId).set(validInsightPayload({ priorityActionTarget: "SETTINGS" }))
  );
  await assertFails(
    oDb.collection("ai_insights").doc(insightDocId).set(validInsightPayload({ priorityActionTitle: "x".repeat(81) }))
  );
  await assertFails(
    oDb.collection("ai_insights").doc(insightDocId).set(validInsightPayload({ id: insightDocId }))
  );
  await assertFails(
    oDb.collection("ai_insights").doc(insightDocId).set(validInsightPayload({ timestamp: new Date(Date.now() + 60000) }))
  );

  // 5. Valid create & update (including negative net profit / deficit) by owner succeeds, plus minimal legacy doc
  await assertSucceeds(oDb.collection("ai_insights").doc(insightDocId).set(validInsightPayload()));
  await assertSucceeds(oDb.collection("ai_insights").doc(insightDocId).get());
  await assertSucceeds(oDb.collection("ai_insights").where("ownerId", "==", OWNER_UID).get());
  await assertSucceeds(
    oDb.collection("ai_insights").doc(insightDocId).set(
      validInsightPayload({
        periodDays: 7,
        healthStatus: "KRITIS",
        labaBersih: -125000,
        marginLabaPersen: -15.4,
        totalOmzet: 1320000,
        totalGalon: 220,
      })
    )
  );
  await assertSucceeds(
    oDb.collection("ai_insights").doc(insightDocId).set({
      ownerId: OWNER_UID,
      depotId: DEPOT_ID,
      insights: ["Omzet stabil."],
      stockAdvice: "Stok aman.",
      totalOmzet: 500000,
      totalGalon: 80,
      activeCustomers: 12,
      modelName: "gemini-3.8-flash",
      timestamp: ts(),
    })
  );

  // 6. Staff & outsider cannot read, list, overwrite, or delete owner's ai_insights
  await assertFails(sDb.collection("ai_insights").doc(insightDocId).get());
  await assertFails(sDb.collection("ai_insights").where("ownerId", "==", OWNER_UID).get());
  await assertFails(sDb.collection("ai_insights").doc(insightDocId).set(validInsightPayload()));
  await assertFails(sDb.collection("ai_insights").doc(insightDocId).delete());

  await assertFails(outDb.collection("ai_insights").doc(insightDocId).get());
  await assertFails(outDb.collection("ai_insights").where("ownerId", "==", OWNER_UID).get());
  await assertFails(outDb.collection("ai_insights").doc(insightDocId).delete());

  // 7. Owner can delete own ai_insight
  await assertSucceeds(oDb.collection("ai_insights").doc(insightDocId).delete());
});

test("malicious other owner cannot hijack an active staff_link belonging to an active depot, but can link after revocation", async () => {
  await seedDepotWithStaff();
  const outDb = outsiderDb();
  const outsiderDepotId = `depot_${OUTSIDER_UID}`;

  // Outsider creates their own valid depot and adds STAFF_EMAIL to memberEmails
  await assertSucceeds(
    outDb.collection("depots").doc(outsiderDepotId).set({
      depotId: outsiderDepotId,
      ownerUid: OUTSIDER_UID,
      ownerEmail: OUTSIDER_EMAIL,
      name: "Depot Lain",
      memberEmails: [STAFF_EMAIL],
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Attempt to hijack active staff_link while STAFF_EMAIL is still active in DEPOT_ID MUST FAIL
  await assertFails(
    outDb.collection("staff_links").doc(STAFF_EMAIL).set({
      depotId: outsiderDepotId,
      ownerUid: OUTSIDER_UID,
      ownerEmail: OUTSIDER_EMAIL,
      staffEmail: STAFF_EMAIL,
      active: true,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Once real owner revokes STAFF_EMAIL from DEPOT_ID, another legitimate depot can claim the email
  await assertSucceeds(
    ownerDb().collection("depots").doc(DEPOT_ID).update({
      memberEmails: [],
      updatedAt: ts(),
    })
  );
  await assertSucceeds(
    outDb.collection("staff_links").doc(STAFF_EMAIL).set({
      depotId: outsiderDepotId,
      ownerUid: OUTSIDER_UID,
      ownerEmail: OUTSIDER_EMAIL,
      staffEmail: STAFF_EMAIL,
      active: true,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
});

test("depot currentTankStockLiters rejects extreme out-of-range corruption while allowing valid updates", async () => {
  await seedDepotWithStaff();
  const oDb = ownerDb();
  const sDb = staffDb(true);

  await assertSucceeds(
    oDb.collection("depots").doc(DEPOT_ID).update({
      currentTankStockLiters: 5000,
      updatedAt: ts(),
    })
  );
  await assertSucceeds(
    sDb.collection("depots").doc(DEPOT_ID).update({
      currentTankStockLiters: 4981,
      updatedAt: ts(),
    })
  );
  await assertFails(
    sDb.collection("depots").doc(DEPOT_ID).update({
      currentTankStockLiters: -500000,
      updatedAt: ts(),
    })
  );
  await assertFails(
    oDb.collection("depots").doc(DEPOT_ID).update({
      currentTankStockLiters: 999999999,
      updatedAt: ts(),
    })
  );
});




