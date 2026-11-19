package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.util.SyncableEntity
import java.util.UUID

@Entity(
    tableName = "transaksi",
    indices = [
        Index("id_pelanggan"),
        Index("id_kurir"),
        Index("id_produk"),
        Index("tanggal"),
        Index("status"),
        Index(value = ["id_pelanggan", "status", "isDeleted"]),
        Index(value = ["syncId"], unique = true)
    ]
)
data class Transaksi(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tanggal: Long,
    val id_pelanggan: Int,
    val id_kurir: Int?,
    val id_produk: Int? = null,
    val jumlah: Int,
    val harga_satuan: Double,
    val total_harga: Double,
    val total_komisi: Double,
    val metode_bayar: String,
    val status: String,
    val uangMuka: Long = 0L,
    val sisaKasbon: Long = 0L,
    val tanggalBayar: Long? = null,
    override val updatedAt: Long = System.currentTimeMillis(),
    override val syncId: String = UUID.randomUUID().toString(),
    val isDeleted: Boolean = false,
    val deletedBy: String? = null,
    val waterType: String = WATER_TYPE_RO
) : SyncableEntity {
    fun remainingKasbonAmount(): Double {
        if (!status.equals("Kasbon", ignoreCase = true)) return 0.0
        return when {
            sisaKasbon > 0L -> sisaKasbon.toDouble()
            uangMuka > 0L || metode_bayar.equals("Bayar Sebagian", ignoreCase = true) -> {
                (total_harga - uangMuka.toDouble()).coerceAtLeast(0.0)
            }
            else -> total_harga
        }
    }

    companion object {
        const val WATER_TYPE_RO = "RO"
        const val WATER_TYPE_MINERAL = "MINERAL"
    }
}
