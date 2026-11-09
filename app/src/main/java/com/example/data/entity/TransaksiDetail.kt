package com.example.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class TransaksiDetail(
    @Embedded val transaksi: Transaksi,
    @Relation(
        parentColumn = "id_pelanggan",
        entityColumn = "id"
    )
    val pelanggan: Pelanggan?,
    @Relation(
        parentColumn = "id_kurir",
        entityColumn = "id"
    )
    val kurir: Kurir?,
    @Relation(
        parentColumn = "id_produk",
        entityColumn = "id"
    )
    val produk: Produk?
)
