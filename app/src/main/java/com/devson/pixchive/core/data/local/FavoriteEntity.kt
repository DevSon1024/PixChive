package com.devson.pixchive.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_images")
data class FavoriteEntity(
    @PrimaryKey
    val uri: String,
    val addedAt: Long = System.currentTimeMillis()
)