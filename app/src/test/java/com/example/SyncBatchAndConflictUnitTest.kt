package com.example

import com.example.data.entity.PendingSyncEntity
import com.example.util.SyncBatchHelper
import com.example.util.SyncCollections
import com.example.util.SyncConflictResolver
import com.example.util.SyncableEntity
import com.google.firebase.firestore.FirebaseFirestoreException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SyncBatchAndConflictUnitTest {

    private data class DummySyncItem(
        override val syncId: String,
        override val updatedAt: Long,
        val label: String
    ) : SyncableEntity

    @Test
    fun conflictResolver_prefersNewerTimestampAndBreaksTiesForLocal() {
        val localOld = DummySyncItem("id-1", 1_000L, "local")
        val remoteNew = DummySyncItem("id-1", 2_000L, "remote")
        assertEquals("remote", SyncConflictResolver.resolve(localOld, remoteNew).label)

        val localNew = DummySyncItem("id-1", 3_000L, "local")
        val remoteOld = DummySyncItem("id-1", 2_000L, "remote")
        assertEquals("local", SyncConflictResolver.resolve(localNew, remoteOld).label)

        val localTie = DummySyncItem("id-1", 5_000L, "local")
        val remoteTie = DummySyncItem("id-1", 5_000L, "remote")
        assertEquals("local", SyncConflictResolver.resolve(localTie, remoteTie).label)
    }

    @Test
    fun sortByPriority_ordersMasterCollectionsBeforeTransactionalAndByTimestamp() {
        val queue = listOf(
            PendingSyncEntity(SyncCollections.TANGKI_REFILL, "ref-1", "UPSERT", 100L),
            PendingSyncEntity(SyncCollections.TRANSAKSI, "tx-2", "UPSERT", 200L),
            PendingSyncEntity(SyncCollections.TRANSAKSI, "tx-1", "UPSERT", 150L),
            PendingSyncEntity(SyncCollections.PELANGGAN, "pel-1", "UPSERT", 300L),
            PendingSyncEntity(SyncCollections.PRODUK, "prod-1", "UPSERT", 400L),
            PendingSyncEntity(SyncCollections.KURIR, "kur-1", "UPSERT", 250L),
            PendingSyncEntity(SyncCollections.KASBON_LOGS, "kas-1", "UPSERT", 120L)
        )

        val ordered = SyncBatchHelper.sortByPriority(queue)
        assertEquals(
            listOf("prod-1", "kur-1", "pel-1", "tx-1", "tx-2", "kas-1", "ref-1"),
            ordered.map { it.syncId }
        )
    }

    @Test
    fun isOwnerOnlyCollection_matchesFirestoreRoleBoundary() {
        assertTrue(SyncBatchHelper.isOwnerOnlyCollection(SyncCollections.PRODUK))
        assertTrue(SyncBatchHelper.isOwnerOnlyCollection(SyncCollections.KURIR))
        assertTrue(SyncBatchHelper.isOwnerOnlyCollection(SyncCollections.PENGELUARAN))
        assertTrue(SyncBatchHelper.isOwnerOnlyCollection(SyncCollections.PERAWATAN_JADWAL))
        assertTrue(SyncBatchHelper.isOwnerOnlyCollection(SyncCollections.PERAWATAN_LOG))
        assertTrue(SyncBatchHelper.isOwnerOnlyCollection(SyncCollections.TANGKI_REFILL))

        assertFalse(SyncBatchHelper.isOwnerOnlyCollection(SyncCollections.PELANGGAN))
        assertFalse(SyncBatchHelper.isOwnerOnlyCollection(SyncCollections.TRANSAKSI))
        assertFalse(SyncBatchHelper.isOwnerOnlyCollection(SyncCollections.KASBON_LOGS))
    }

    @Test
    fun partitionIntoBatches_splitsByCollectionAndRespectsMaxSize() {
        val ordered = listOf(
            PendingSyncEntity(SyncCollections.PELANGGAN, "p1", "UPSERT", 1L),
            PendingSyncEntity(SyncCollections.PELANGGAN, "p2", "UPSERT", 2L),
            PendingSyncEntity(SyncCollections.PELANGGAN, "p3", "UPSERT", 3L),
            PendingSyncEntity(SyncCollections.TRANSAKSI, "t1", "UPSERT", 4L),
            PendingSyncEntity(SyncCollections.TRANSAKSI, "t2", "UPSERT", 5L)
        )

        val batches = SyncBatchHelper.partitionIntoBatches(ordered, maxBatchSize = 2)
        assertEquals(3, batches.size)
        assertEquals(listOf("p1", "p2"), batches[0].map { it.syncId })
        assertEquals(listOf("p3"), batches[1].map { it.syncId })
        assertEquals(listOf("t1", "t2"), batches[2].map { it.syncId })
        assertTrue(SyncBatchHelper.partitionIntoBatches(emptyList()).isEmpty())
    }

    @Test
    fun shouldFallbackToSingleItem_trueForRuleOrValidationErrorsFalseForTransientNetwork() {
        assertTrue(SyncBatchHelper.shouldFallbackToSingleItem(FirebaseFirestoreException.Code.PERMISSION_DENIED))
        assertTrue(SyncBatchHelper.shouldFallbackToSingleItem(FirebaseFirestoreException.Code.INVALID_ARGUMENT))
        assertTrue(SyncBatchHelper.shouldFallbackToSingleItem(FirebaseFirestoreException.Code.OUT_OF_RANGE))
        assertTrue(SyncBatchHelper.shouldFallbackToSingleItem(FirebaseFirestoreException.Code.DATA_LOSS))
        assertTrue(SyncBatchHelper.shouldFallbackToSingleItem(FirebaseFirestoreException.Code.FAILED_PRECONDITION))

        assertFalse(SyncBatchHelper.shouldFallbackToSingleItem(FirebaseFirestoreException.Code.UNAVAILABLE))
        assertFalse(SyncBatchHelper.shouldFallbackToSingleItem(FirebaseFirestoreException.Code.DEADLINE_EXCEEDED))
        assertFalse(SyncBatchHelper.shouldFallbackToSingleItem(null))
    }
}
