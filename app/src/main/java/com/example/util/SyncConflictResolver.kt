package com.example.util

/**
 * Kontrak untuk seluruh entitas Room yang disinkronkan dua arah dengan Firestore.
 */
interface SyncableEntity {
    val updatedAt: Long
    val syncId: String
}

/**
 * Penentu resolusi konflik sinkronisasi dua arah menggunakan strategi Last-Write-Wins
 * berbasis timestamp [SyncableEntity.updatedAt].
 */
object SyncConflictResolver {
    /**
     * Memilih entitas dengan pembaruan terbaru antara [local] dan [remote].
     * Jika timestamp sama, [local] dipertahankan.
     */
    fun <T : SyncableEntity> resolve(local: T, remote: T): T {
        return if (local.updatedAt >= remote.updatedAt) local else remote
    }
}
