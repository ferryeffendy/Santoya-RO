package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Jadwal perawatan berkala mesin RO depot (akses khusus pemilik).
 */
@Entity(
    tableName = "perawatan_item",
    indices = [Index(value = ["syncId"], unique = true)]
)
data class PerawatanItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String = "",
    val intervalHari: Int = 30,
    val terakhirSelesai: Long? = null,
    val jadwalBerikutnya: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncId: String = UUID.randomUUID().toString()
)
