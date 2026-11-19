package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class DepotFirestoreRuleTest : FirestoreEmulatorTestBase() {

    private suspend fun setupOwnerDepot(
        ownerUid: String,
        ownerEmail: String,
        staffEmails: List<String> = emptyList()
    ): String {
        val depotId = "depot_${ownerUid}_${UUID.randomUUID().toString().take(8)}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("depots").document(depotId).set(
                mapOf(
                    "depotId" to depotId,
                    "ownerUid" to ownerUid,
                    "ownerEmail" to ownerEmail,
                    "name" to "Santoya RO",
                    "memberEmails" to staffEmails,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        return depotId
    }

    @Test
    fun ownerCanCreateDepotAndStaffLinkAndWriteOwnerCollections() = runBlocking {
        val ownerUid = signInTestUser(OWNER_EMAIL, verifyEmail = true)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("staff_links").document(OWNER_EMAIL).get().await()
            firestore.collection("depots").document("depot_$ownerUid").get().await()
            firestore.collection("depots").document("depot_$ownerUid").collection("produk").get().await()
        }
        val depotId = setupOwnerDepot(ownerUid, OWNER_EMAIL, listOf(STAFF_EMAIL))

        val linkKey = "${STAFF_EMAIL.substringBefore("@")}_${UUID.randomUUID().toString().take(6)}@test.com"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("staff_links").document(linkKey).set(
                mapOf(
                    "depotId" to depotId,
                    "ownerUid" to ownerUid,
                    "ownerEmail" to OWNER_EMAIL,
                    "staffEmail" to linkKey,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val produkSyncId = "prod_${UUID.randomUUID()}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("depots").document(depotId)
                .collection("produk").document(produkSyncId)
                .set(
                    mapOf(
                        "syncId" to produkSyncId,
                        "nama_produk" to "Galon RO",
                        "harga" to 6000.0,
                        "updatedAt" to 100L,
                        "isDeleted" to false,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        }

        val expSyncId = "exp_${UUID.randomUUID()}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("depots").document(depotId)
                .collection("pengeluaran").document(expSyncId)
                .set(
                    mapOf(
                        "syncId" to expSyncId,
                        "tanggal" to 1000L,
                        "nama_item" to "Filter Sedimen",
                        "kategori" to "Operasional",
                        "nominal" to 50000.0,
                        "updatedAt" to 100L,
                        "isDeleted" to false,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        }

        val snap = withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("depots").document(depotId)
                .collection("produk").document(produkSyncId).get().await()
        }
        assertTrue(snap.exists())
        assertEquals("Galon RO", snap.getString("nama_produk"))

        // Simulate shadow owner hijacking staff_links/{STAFF_EMAIL}, then real owner reclaiming it
        val shadowOwnerUid = signInTestUser(OUTSIDER_EMAIL, verifyEmail = true)
        val shadowDepotId = setupOwnerDepot(shadowOwnerUid, OUTSIDER_EMAIL, listOf(STAFF_EMAIL))
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("staff_links").document(STAFF_EMAIL).set(
                mapOf(
                    "depotId" to shadowDepotId,
                    "ownerUid" to shadowOwnerUid,
                    "ownerEmail" to OUTSIDER_EMAIL,
                    "staffEmail" to STAFF_EMAIL,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        // Real owner signs back in and reclaims staff_links/{STAFF_EMAIL} pointing to real depotId
        signInTestUser(OWNER_EMAIL, verifyEmail = true)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("staff_links").document(STAFF_EMAIL).set(
                mapOf(
                    "depotId" to depotId,
                    "ownerUid" to ownerUid,
                    "ownerEmail" to OWNER_EMAIL,
                    "staffEmail" to STAFF_EMAIL,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val reclaimedSnap = withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("staff_links").document(STAFF_EMAIL).get().await()
        }
        assertEquals(depotId, reclaimedSnap.getString("depotId"))
        assertEquals(OWNER_EMAIL, reclaimedSnap.getString("ownerEmail"))
    }

    @Test
    fun verifiedStaffCanReadAndWriteTransaksiButCannotAccessPengeluaranOrProdukWrite() = runBlocking {
        val ownerUid = signInTestUser(OWNER_EMAIL, verifyEmail = true)
        val depotId = setupOwnerDepot(ownerUid, OWNER_EMAIL, listOf(STAFF_EMAIL))

        signInTestUser(STAFF_EMAIL, verifyEmail = true)

        val pelangganSyncId = "pel_${UUID.randomUUID()}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("depots").document(depotId)
                .collection("pelanggan").document(pelangganSyncId)
                .set(
                    mapOf(
                        "syncId" to pelangganSyncId,
                        "nama" to "Ibu Sari",
                        "alamat" to "Jl. Merpati",
                        "no_hp" to "08123",
                        "sisa_kasbon" to 0.0,
                        "harga_khusus" to null,
                        "kurirSyncId" to null,
                        "updatedAt" to 200L,
                        "isDeleted" to false,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        }

        val txSyncId = "tx_${UUID.randomUUID()}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("depots").document(depotId)
                .collection("transaksi").document(txSyncId)
                .set(
                    mapOf(
                        "syncId" to txSyncId,
                        "tanggal" to 2000L,
                        "pelangganSyncId" to pelangganSyncId,
                        "kurirSyncId" to null,
                        "produkSyncId" to null,
                        "jumlah" to 2,
                        "harga_satuan" to 6000.0,
                        "total_harga" to 12000.0,
                        "total_komisi" to 0.0,
                        "metode_bayar" to "Cash",
                        "status" to "Lunas",
                        "uangMuka" to 0L,
                        "sisaKasbon" to 0L,
                        "updatedAt" to 200L,
                        "isDeleted" to false,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        }

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("depots").document(depotId)
                    .collection("pengeluaran").get().await()
            }
            fail("Expected PERMISSION_DENIED for staff reading pengeluaran")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException() ?: throw e
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe.code)
        }

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("depots").document(depotId)
                    .collection("produk").document("prod_hack")
                    .set(
                        mapOf(
                            "syncId" to "prod_hack",
                            "nama_produk" to "Hack",
                            "harga" to 1.0,
                            "updatedAt" to 200L,
                            "isDeleted" to false,
                            "serverUpdatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
            fail("Expected PERMISSION_DENIED for staff writing produk")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException() ?: throw e
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe.code)
        }
    }

    @Test
    fun unauthenticatedAndOutsiderUsersAreDenied() = runBlocking {
        val ownerUid = signInTestUser(OWNER_EMAIL, verifyEmail = true)
        val depotId = setupOwnerDepot(ownerUid, OWNER_EMAIL, listOf(STAFF_EMAIL))

        signInTestUser(OUTSIDER_EMAIL, verifyEmail = true)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("depots").document(depotId)
                    .collection("pelanggan").get().await()
            }
            fail("Expected PERMISSION_DENIED for outsider")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException() ?: throw e
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe.code)
        }

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("staff_links").document("nonexistent_probe@test.com").get().await()
            }
            fail("Expected PERMISSION_DENIED for outsider probing non-existent staff_links")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException() ?: throw e
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe.code)
        }

        auth.signOut()
        val unauthFlow = callbackFlow {
            val reg = firestore.collection("depots").document(depotId)
                .collection("transaksi")
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        close(err)
                    } else if (snap != null) {
                        trySend(snap.size())
                    }
                }
            awaitClose { reg.remove() }
        }
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                unauthFlow.first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated listener")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException() ?: throw e
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe.code)
        }
    }

    @Test
    fun ownerCanManagePerawatanAndStaffIsDeniedPerawatanReadAndWrite() = runBlocking {
        val ownerUid = signInTestUser(OWNER_EMAIL, verifyEmail = true)
        val depotId = setupOwnerDepot(ownerUid, OWNER_EMAIL, listOf(STAFF_EMAIL))

        val jadwalId = "pw_${UUID.randomUUID()}"
        val logId = "pwlog_${UUID.randomUUID()}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("depots").document(depotId)
                .collection("perawatan_jadwal").document(jadwalId)
                .set(
                    mapOf(
                        "syncId" to jadwalId,
                        "nama" to "Filter Sedimen",
                        "intervalHari" to 30,
                        "terakhirSelesai" to null,
                        "jadwalBerikutnya" to 1702592000000L,
                        "updatedAt" to 300L,
                        "isDeleted" to false,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            firestore.collection("depots").document(depotId)
                .collection("perawatan_log").document(logId)
                .set(
                    mapOf(
                        "syncId" to logId,
                        "perawatanSyncId" to jadwalId,
                        "namaPerawatan" to "Filter Sedimen",
                        "tanggalSelesai" to 1700000000000L,
                        "biaya" to 75000.0,
                        "catatan" to "Ganti filter rutin",
                        "pengeluaranSyncId" to "exp_1",
                        "updatedAt" to 300L,
                        "isDeleted" to false,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        }

        val ownerSnap = withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("depots").document(depotId)
                .collection("perawatan_jadwal").document(jadwalId).get().await()
        }
        assertTrue(ownerSnap.exists())
        assertEquals("Filter Sedimen", ownerSnap.getString("nama"))

        // Switch to verified active staff: must be denied read and write on perawatan collections
        signInTestUser(STAFF_EMAIL, verifyEmail = true)

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("depots").document(depotId)
                    .collection("perawatan_jadwal").get().await()
            }
            fail("Expected PERMISSION_DENIED for staff listing perawatan_jadwal")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException() ?: throw e
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe.code)
        }

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("depots").document(depotId)
                    .collection("perawatan_jadwal").document("pw_hack")
                    .set(
                        mapOf(
                            "syncId" to "pw_hack",
                            "nama" to "Hack",
                            "intervalHari" to 7,
                            "terakhirSelesai" to null,
                            "jadwalBerikutnya" to 1700000000000L,
                            "updatedAt" to 400L,
                            "isDeleted" to false,
                            "serverUpdatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
            fail("Expected PERMISSION_DENIED for staff writing perawatan_jadwal")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException() ?: throw e
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe.code)
        }

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("depots").document(depotId)
                    .collection("perawatan_log").get().await()
            }
            fail("Expected PERMISSION_DENIED for staff listing perawatan_log")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException() ?: throw e
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe.code)
        }

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("depots").document(depotId)
                    .collection("perawatan_log").document("pwlog_hack")
                    .set(
                        mapOf(
                            "syncId" to "pwlog_hack",
                            "perawatanSyncId" to jadwalId,
                            "namaPerawatan" to "Filter Sedimen",
                            "tanggalSelesai" to 1700000000000L,
                            "biaya" to 0.0,
                            "catatan" to "Hack",
                            "pengeluaranSyncId" to null,
                            "updatedAt" to 400L,
                            "isDeleted" to false,
                            "serverUpdatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
            fail("Expected PERMISSION_DENIED for staff writing perawatan_log")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException() ?: throw e
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe.code)
        }
    }

    @Test
    fun mathValidationEnforcesConsistencyForTransaksiAndKasbonLogs() = runBlocking {
        val ownerUid = signInTestUser(OWNER_EMAIL, verifyEmail = true)
        val depotId = setupOwnerDepot(ownerUid, OWNER_EMAIL, listOf(STAFF_EMAIL))
        signInTestUser(STAFF_EMAIL, verifyEmail = true)

        val txKasbonId = "tx_kasbon_${UUID.randomUUID()}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("depots").document(depotId)
                .collection("transaksi").document(txKasbonId)
                .set(
                    mapOf(
                        "syncId" to txKasbonId,
                        "tanggal" to 2000L,
                        "pelangganSyncId" to "pel_1",
                        "kurirSyncId" to null,
                        "produkSyncId" to null,
                        "jumlah" to 3,
                        "harga_satuan" to 6000.0,
                        "total_harga" to 18000.0,
                        "total_komisi" to 3000.0,
                        "metode_bayar" to "Bayar Sebagian",
                        "status" to "Kasbon",
                        "uangMuka" to 5000L,
                        "sisaKasbon" to 13000L,
                        "updatedAt" to 200L,
                        "isDeleted" to false,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        }

        val logValidId = "klog_ok_${UUID.randomUUID()}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("depots").document(depotId)
                .collection("kasbon_logs").document(logValidId)
                .set(
                    mapOf(
                        "syncId" to logValidId,
                        "pelangganSyncId" to "pel_1",
                        "tanggal" to 2100L,
                        "jumlah_bayar" to 5000.0,
                        "sisa_kasbon_sebelum" to 13000.0,
                        "sisa_kasbon_sesudah" to 8000.0,
                        "metode_bayar" to "Cash",
                        "catatan" to "Cicilan pertama",
                        "updatedAt" to 210L,
                        "isDeleted" to false,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        }

        // Tampered total_harga should be denied
        try {
            val badTotalId = "tx_bad_total_${UUID.randomUUID()}"
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("depots").document(depotId)
                    .collection("transaksi").document(badTotalId)
                    .set(
                        mapOf(
                            "syncId" to badTotalId,
                            "tanggal" to 2000L,
                            "pelangganSyncId" to "pel_1",
                            "kurirSyncId" to null,
                            "produkSyncId" to null,
                            "jumlah" to 3,
                            "harga_satuan" to 6000.0,
                            "total_harga" to 1000.0,
                            "total_komisi" to 0.0,
                            "metode_bayar" to "Cash",
                            "status" to "Lunas",
                            "uangMuka" to 0L,
                            "sisaKasbon" to 0L,
                            "updatedAt" to 200L,
                            "isDeleted" to false,
                            "serverUpdatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
            fail("Expected PERMISSION_DENIED for manipulated total_harga")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException() ?: throw e
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe.code)
        }

        // Inconsistent kasbon sisaKasbon should be denied
        try {
            val badKasbonId = "tx_bad_kasbon_${UUID.randomUUID()}"
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("depots").document(depotId)
                    .collection("transaksi").document(badKasbonId)
                    .set(
                        mapOf(
                            "syncId" to badKasbonId,
                            "tanggal" to 2000L,
                            "pelangganSyncId" to "pel_1",
                            "kurirSyncId" to null,
                            "produkSyncId" to null,
                            "jumlah" to 3,
                            "harga_satuan" to 6000.0,
                            "total_harga" to 18000.0,
                            "total_komisi" to 0.0,
                            "metode_bayar" to "Bayar Sebagian",
                            "status" to "Kasbon",
                            "uangMuka" to 5000L,
                            "sisaKasbon" to 1000L,
                            "updatedAt" to 200L,
                            "isDeleted" to false,
                            "serverUpdatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
            fail("Expected PERMISSION_DENIED for inconsistent sisaKasbon")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException() ?: throw e
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe.code)
        }

        // Tampered kasbon_log math should be denied
        try {
            val badLogId = "klog_bad_${UUID.randomUUID()}"
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("depots").document(depotId)
                    .collection("kasbon_logs").document(badLogId)
                    .set(
                        mapOf(
                            "syncId" to badLogId,
                            "pelangganSyncId" to "pel_1",
                            "tanggal" to 2100L,
                            "jumlah_bayar" to 5000.0,
                            "sisa_kasbon_sebelum" to 13000.0,
                            "sisa_kasbon_sesudah" to 0.0,
                            "metode_bayar" to "Cash",
                            "catatan" to "Manipulasi",
                            "updatedAt" to 210L,
                            "isDeleted" to false,
                            "serverUpdatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
            fail("Expected PERMISSION_DENIED for tampered kasbon_log sisa_kasbon_sesudah")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException() ?: throw e
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe.code)
        }
    }

    private companion object {
        const val OWNER_EMAIL = "owner_rule@test.com"
        const val STAFF_EMAIL = "staff_rule@test.com"
        const val OUTSIDER_EMAIL = "outsider_rule@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
