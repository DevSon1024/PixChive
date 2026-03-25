package com.devson.pixchive.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "images",
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["dateModified"]),
        Index(value = ["parentFolderPath", "name"])
    ]
)
data class ImageEntity(
    @PrimaryKey val path: String,
    val folderId: String,          // The ID of the root folder imported (e.g., "UUID...")
    val uri: String = "",          // SAF content URI string (for SAF-granted folders)
    val name: String,
    val size: Long,
    val dateModified: Long,
    val parentFolderPath: String,  // Used to group images into chapters
    val parentFolderName: String,  // The display name of the chapter
    val formattedSize: String = "" // Pre-computed human-readable file size (e.g. "1.4 MB")
)