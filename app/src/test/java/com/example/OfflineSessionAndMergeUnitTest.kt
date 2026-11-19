package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.entity.DepotProfile
import com.example.util.DepotProfileStore
import com.example.util.DepotRole
import com.example.util.OfflineSessionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfflineSessionAndMergeUnitTest {

    private lateinit var context: Context
    private lateinit var sessionManager: OfflineSessionManager
    private lateinit var profileStore: DepotProfileStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sessionManager = OfflineSessionManager(context)
        sessionManager.clearAll()
        profileStore = DepotProfileStore(context)
        profileStore.clearProfile(OfflineSessionManager.OFFLINE_LOCAL_UID)
        profileStore.clearProfile("google_uid_1")
        profileStore.clearProfile("google_uid_2")
        profileStore.clearActiveSession()
    }

    @Test
    fun `offline session manager tracks last session and merge eligibility accurately`() {
        assertFalse(sessionManager.isOfflineModeActive())
        assertFalse(sessionManager.hasPendingOfflineMerge())
        assertNull(sessionManager.getLastSession())

        sessionManager.setOfflineModeActive(true)
        sessionManager.setHasPendingOfflineMerge(true)
        sessionManager.saveLastSession(
            uid = OfflineSessionManager.OFFLINE_LOCAL_UID,
            email = "",
            displayName = "Depot Santoya Offline",
            isOfflineLocal = true,
            role = DepotRole.OWNER
        )

        assertTrue(sessionManager.isOfflineModeActive())
        assertTrue(sessionManager.hasPendingOfflineMerge())
        val last = sessionManager.getLastSession()
        assertNotNull(last)
        assertEquals(OfflineSessionManager.OFFLINE_LOCAL_UID, last?.uid)
        assertTrue(last?.isOfflineLocal == true)
        assertEquals("Depot Santoya Offline", last?.displayName)
        assertEquals(DepotRole.OWNER, last?.role)

        assertTrue(
            sessionManager.shouldPreserveLocalDataOnGoogleSignIn(
                newUid = "google_uid_1",
                previousOwnerUid = OfflineSessionManager.OFFLINE_LOCAL_UID
            )
        )
        assertTrue(
            sessionManager.shouldPreserveLocalDataOnGoogleSignIn(
                newUid = "google_uid_1",
                previousOwnerUid = null
            )
        )
        assertTrue(
            sessionManager.shouldPreserveLocalDataOnGoogleSignIn(
                newUid = "google_uid_1",
                previousOwnerUid = "google_uid_1"
            )
        )
        assertFalse(
            sessionManager.shouldPreserveLocalDataOnGoogleSignIn(
                newUid = "google_uid_1",
                previousOwnerUid = "other_google_uid"
            )
        )
        assertFalse(
            sessionManager.shouldPreserveLocalDataOnGoogleSignIn(
                newUid = OfflineSessionManager.OFFLINE_LOCAL_UID,
                previousOwnerUid = OfflineSessionManager.OFFLINE_LOCAL_UID
            )
        )

        sessionManager.setHasPendingOfflineMerge(false)
        assertFalse(
            sessionManager.shouldPreserveLocalDataOnGoogleSignIn(
                newUid = "google_uid_1",
                previousOwnerUid = OfflineSessionManager.OFFLINE_LOCAL_UID
            )
        )

        sessionManager.clearAll()
        assertFalse(sessionManager.isOfflineModeActive())
        assertNull(sessionManager.getLastSession())
    }

    @Test
    fun `depot profile store merges offline edits into new google uid without overwriting existing target fields`() {
        profileStore.loadForUser(
            uid = OfflineSessionManager.OFFLINE_LOCAL_UID,
            email = "",
            googleDisplayName = "Pemilik Offline",
            appVersion = "1.0"
        )
        profileStore.saveOwnerEdit(
            uid = OfflineSessionManager.OFFLINE_LOCAL_UID,
            ownerName = "Pak Budi",
            depotName = "Tirta Jaya RO",
            phoneNumber = "081234567890",
            address = "Jl. Melati No. 10",
            receiptFooter = "Terima kasih langganan kami",
            depotType = DepotProfile.TYPE_BOTH,
            tankCapacityLiters = 7500.0
        )
        profileStore.setTankStock(
            uid = OfflineSessionManager.OFFLINE_LOCAL_UID,
            newStockLiters = 3200.0,
            newCapacityLiters = 7500.0
        )

        val mergedIntoBlank = profileStore.mergeFromOfflineProfile(
            offlineUid = OfflineSessionManager.OFFLINE_LOCAL_UID,
            targetUid = "google_uid_1",
            email = "budi@gmail.com",
            googleDisplayName = "Budi Google",
            appVersion = "1.0"
        )

        assertEquals("google_uid_1", mergedIntoBlank.uid)
        assertEquals("Pak Budi", mergedIntoBlank.ownerName)
        assertEquals("Tirta Jaya RO", mergedIntoBlank.depotName)
        assertEquals("081234567890", mergedIntoBlank.phoneNumber)
        assertEquals("Jl. Melati No. 10", mergedIntoBlank.address)
        assertEquals("Terima kasih langganan kami", mergedIntoBlank.receiptFooter)
        assertEquals(DepotProfile.TYPE_BOTH, mergedIntoBlank.depotType)
        assertEquals(7500.0, mergedIntoBlank.tankCapacityLiters, 0.01)
        assertEquals(3200.0, mergedIntoBlank.currentTankStockLiters, 0.01)
        assertTrue(profileStore.hasPendingSync("google_uid_1"))

        profileStore.loadForUser(
            uid = "google_uid_2",
            email = "existing@gmail.com",
            googleDisplayName = "Existing Owner",
            appVersion = "1.0"
        )
        profileStore.saveOwnerEdit(
            uid = "google_uid_2",
            ownerName = "Existing Owner",
            depotName = "Depot Lama Cloud",
            phoneNumber = "089900112233",
            address = "Jl. Merdeka No. 1",
            receiptFooter = "Footer Cloud"
        )

        val mergedIntoExisting = profileStore.mergeFromOfflineProfile(
            offlineUid = OfflineSessionManager.OFFLINE_LOCAL_UID,
            targetUid = "google_uid_2",
            email = "existing@gmail.com",
            googleDisplayName = "Existing Owner",
            appVersion = "1.0"
        )

        assertEquals("Depot Lama Cloud", mergedIntoExisting.depotName)
        assertEquals("089900112233", mergedIntoExisting.phoneNumber)
        assertEquals("Jl. Merdeka No. 1", mergedIntoExisting.address)
        assertEquals("Footer Cloud", mergedIntoExisting.receiptFooter)

        profileStore.clearProfile(OfflineSessionManager.OFFLINE_LOCAL_UID)
        val reloadedOffline = profileStore.loadForUser(
            uid = OfflineSessionManager.OFFLINE_LOCAL_UID,
            email = "",
            googleDisplayName = "",
            appVersion = "1.0"
        )
        assertEquals("", reloadedOffline.depotName)
        assertFalse(profileStore.hasPendingSync(OfflineSessionManager.OFFLINE_LOCAL_UID))
    }

    @Test
    fun `sync summary total records count and ordered collection keys include all domain collections`() {
        val summary = com.example.util.SyncSummary(
            pelangganCount = 12,
            transaksiCount = 45,
            produkCount = 4,
            kurirCount = 3,
            pengeluaranCount = 8,
            kasbonLogCount = 10,
            perawatanCount = 11,
            tangkiRefillCount = 2,
            pushedCount = 95
        )
        assertEquals(95, summary.totalRecordsCount())
        assertEquals(95, summary.pushedCount)

        val keys = com.example.util.SyncCollections.ORDERED_DISPLAY_KEYS
        assertTrue(keys.contains(com.example.util.SyncCollections.TRANSAKSI))
        assertTrue(keys.contains(com.example.util.SyncCollections.PELANGGAN))
        assertTrue(keys.contains(com.example.util.SyncCollections.KASBON_LOGS))
        assertTrue(keys.contains(com.example.util.SyncCollections.PRODUK))
        assertTrue(keys.contains(com.example.util.SyncCollections.KURIR))
        assertTrue(keys.contains(com.example.util.SyncCollections.PENGELUARAN))
        assertTrue(keys.contains(com.example.util.SyncCollections.PERAWATAN_JADWAL))
        assertTrue(keys.contains(com.example.util.SyncCollections.PERAWATAN_LOG))
        assertTrue(keys.contains(com.example.util.SyncCollections.TANGKI_REFILL))
        assertTrue(keys.contains(com.example.util.SyncCollections.PROFIL))
    }
}
