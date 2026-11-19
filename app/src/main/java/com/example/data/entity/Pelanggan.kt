package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.util.SyncableEntity
import java.util.UUID

@Entity(
    tableName = "pelanggan",
    indices = [
        Index(value = ["syncId"], unique = true),
        Index("id_kurir_langganan"),
        Index("isDeleted")
    ]
)
data class Pelanggan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String = "",
    val alamat: String = "",
    val no_hp: String = "",
    val sisa_kasbon: Double = 0.0,
    val harga_khusus: Double? = null,
    val id_kurir_langganan: Int? = null,
    override val updatedAt: Long = System.currentTimeMillis(),
    override val syncId: String = UUID.randomUUID().toString(),
    val isDeleted: Boolean = false,
    val deletedBy: String? = null
) : SyncableEntity
