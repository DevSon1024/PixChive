package com.devson.pixchive.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks the user's reading progress for recently visited chapters.
 * Stored in Room so it survives app restarts.
 */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val chapterPath: String,
    val folderId: String,
    val title: String,
    val coverImageUri: String,
    val currentPage: Int,
    val totalPages: Int,
    val lastAccessed: Long
)
