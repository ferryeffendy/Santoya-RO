package com.example.data.entity

import androidx.room.Entity

@Entity(
    tableName = "pending_sync",
    primaryKeys = ["collectionName", "syncId"]
)
data class PendingSyncEntity(
    val collectionName: String,
    val syncId: String,
    val operation: String,
    val updatedAt: Long = System.currentTimeMillis()
)
