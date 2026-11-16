package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.util.SyncableEntity
import java.util.UUID

/**
 * Riwayat pelaksanaan perawatan mesin RO depot (akses khusus pemilik).
 */
@Entity(
    tableName = "perawatan_log",
    indices = [
        Index(value = ["syncId"], unique = true),
        Index(value = ["perawatanSyncId"])
    ]
)
data class PerawatanLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val perawatanSyncId: String = "",
    val namaPerawatan: String = "",
    val tanggalSelesai: Long = 0L,
    val biaya: Double = 0.0,
    val catatan: String = "",
    val pengeluaranSyncId: String? = null,
    override val updatedAt: Long = System.currentTimeMillis(),
    override val syncId: String = UUID.randomUUID().toString()
) : SyncableEntity
