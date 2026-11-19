package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.util.SyncableEntity
import java.util.UUID

/**
 * Entitas riwayat pembelian air tangki untuk pengisian toren air mineral / air gunung.
 * Dapat terhubung secara dua arah ke pengeluaran operasional (linkedExpenseId & pengeluaranSyncId).
 */
@Entity(
    tableName = "tangki_refill",
    indices = [
        Index("tanggal"),
        Index(value = ["syncId"], unique = true),
        Index("linkedExpenseId")
    ]
)
data class TangkiRefill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tanggal: Long,
    val volumeLiter: Double,
    val totalHarga: Double,
    val namaPemasok: String = "",
    val catatan: String = "",
    val linkedExpenseId: Int? = null,
    val pengeluaranSyncId: String? = null,
    override val updatedAt: Long = System.currentTimeMillis(),
    override val syncId: String = UUID.randomUUID().toString(),
    val isDeleted: Boolean = false,
    val deletedBy: String? = null
) : SyncableEntity
