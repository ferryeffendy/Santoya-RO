package com.example.util

import com.example.data.entity.PendingSyncEntity
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Helper murni untuk mengurutkan, memfilter berdasarkan peran, dan mempartisi antrean sinkronisasi
 * offline menjadi kelompok batch aman untuk Firestore [com.google.firebase.firestore.WriteBatch].
 */
object SyncBatchHelper {

    /** Batas operasi per WriteBatch agar responsif sekaligus jauh di bawah batas 500 operasi Firestore. */
    const val DEFAULT_BATCH_SIZE = 50

    private val COLLECTION_PRIORITY: Map<String, Int> = mapOf(
        SyncCollections.PRODUK to 0,
        SyncCollections.KURIR to 1,
        SyncCollections.PELANGGAN to 2,
        SyncCollections.TRANSAKSI to 3,
        SyncCollections.KASBON_LOGS to 4,
        SyncCollections.PENGELUARAN to 5,
        SyncCollections.PERAWATAN_JADWAL to 6,
        SyncCollections.PERAWATAN_LOG to 7,
        SyncCollections.TANGKI_REFILL to 8
    )

    private val OWNER_ONLY_COLLECTIONS: Set<String> = setOf(
        SyncCollections.PENGELUARAN,
        SyncCollections.PERAWATAN_JADWAL,
        SyncCollections.PERAWATAN_LOG,
        SyncCollections.TANGKI_REFILL,
        SyncCollections.PRODUK,
        SyncCollections.KURIR
    )

    /**
     * Mengembalikan true jika [collectionName] hanya boleh dimodifikasi oleh Pemilik depot.
     */
    fun isOwnerOnlyCollection(collectionName: String): Boolean =
        collectionName in OWNER_ONLY_COLLECTIONS

    /**
     * Mengurutkan daftar antrean berdasarkan dependensi relasional koleksi (master data lebih dahulu)
     * lalu berdasarkan waktu pembaruan paling awal.
     */
    fun sortByPriority(items: List<PendingSyncEntity>): List<PendingSyncEntity> {
        return items.sortedWith(
            compareBy<PendingSyncEntity> { COLLECTION_PRIORITY[it.collectionName] ?: 99 }
                .thenBy { it.updatedAt }
        )
    }

    /**
     * Mempartisi daftar antrean terurut menjadi potongan-potongan homogen per koleksi dengan ukuran
     * maksimal [maxBatchSize] agar urutan dependensi antar koleksi tetap terjaga ketat.
     */
    fun partitionIntoBatches(
        orderedItems: List<PendingSyncEntity>,
        maxBatchSize: Int = DEFAULT_BATCH_SIZE
    ): List<List<PendingSyncEntity>> {
        if (orderedItems.isEmpty()) return emptyList()
        val safeMax = maxBatchSize.coerceAtLeast(1)
        val result = mutableListOf<List<PendingSyncEntity>>()
        var currentBatch = mutableListOf<PendingSyncEntity>()
        var currentCollection: String? = null

        for (item in orderedItems) {
            val collectionChanged = currentCollection != null && currentCollection != item.collectionName
            if (collectionChanged || currentBatch.size >= safeMax) {
                result.add(currentBatch)
                currentBatch = mutableListOf()
            }
            currentBatch.add(item)
            currentCollection = item.collectionName
        }
        if (currentBatch.isNotEmpty()) {
            result.add(currentBatch)
        }
        return result
    }

    /**
     * Menentukan apakah kegagalan commit WriteBatch perlu mundur (*fallback*) ke penulisan per-item
     * agar satu dokumen cacat atau ditolak aturan tidak memblokir dokumen valid lainnya.
     */
    fun shouldFallbackToSingleItem(code: FirebaseFirestoreException.Code?): Boolean {
        return code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
            code == FirebaseFirestoreException.Code.INVALID_ARGUMENT ||
            code == FirebaseFirestoreException.Code.OUT_OF_RANGE ||
            code == FirebaseFirestoreException.Code.DATA_LOSS ||
            code == FirebaseFirestoreException.Code.FAILED_PRECONDITION
    }
}
