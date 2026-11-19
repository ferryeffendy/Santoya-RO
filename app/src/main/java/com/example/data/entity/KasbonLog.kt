package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.util.SyncableEntity
import java.util.UUID

@Entity(
    tableName = "kasbon_log",
    indices = [
        Index(value = ["syncId"], unique = true),
        Index("id_pelanggan"),
        Index("tanggal")
    ]
)
data class KasbonLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val id_pelanggan: Int,
    val tanggal: Long = System.currentTimeMillis(),
    val jumlah_bayar: Double,
    val sisa_kasbon_sebelum: Double,
    val sisa_kasbon_sesudah: Double,
    val metode_bayar: String = "Cash",
    val catatan: String = "",
    override val updatedAt: Long = System.currentTimeMillis(),
    override val syncId: String = UUID.randomUUID().toString()
) : SyncableEntity
