package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "transaksi",
    indices = [
        Index("id_pelanggan"),
        Index("id_kurir"),
        Index("id_produk"),
        Index("tanggal"),
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
    val updatedAt: Long = System.currentTimeMillis(),
    val syncId: String = UUID.randomUUID().toString()
) {
    fun remainingKasbonAmount(): Double {
        if (!status.equals("Kasbon", ignoreCase = true)) return 0.0
        return if (sisaKasbon > 0L) sisaKasbon.toDouble() else total_harga
    }
}
