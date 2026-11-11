package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaksi",
    foreignKeys = [
        ForeignKey(
            entity = Pelanggan::class,
            parentColumns = ["id"],
            childColumns = ["id_pelanggan"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Kurir::class,
            parentColumns = ["id"],
            childColumns = ["id_kurir"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Produk::class,
            parentColumns = ["id"],
            childColumns = ["id_produk"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("id_pelanggan"),
        Index("id_kurir"),
        Index("id_produk"),
        Index("tanggal")
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
    val metode_bayar: String, // Cash, Transfer, Bayar Sebagian
    val status: String, // Lunas, Kasbon
    val uangMuka: Long = 0L,
    val sisaKasbon: Long = 0L
) {
    fun remainingKasbonAmount(): Double {
        if (!status.equals("Kasbon", ignoreCase = true)) return 0.0
        return if (sisaKasbon > 0L) sisaKasbon.toDouble() else total_harga
    }
}
