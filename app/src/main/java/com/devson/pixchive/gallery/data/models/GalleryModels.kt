package com.devson.pixchive.gallery.data.models

import android.net.Uri

data class GalleryFolder(
    val bucketId: String,
    val folderName: String,
    val thumbnailUri: Uri, // Points to the most recent image in the folder
    val imageCount: Int
)

data class GalleryImage(
    val id: Long,
    val uri: Uri,          // content:// uri for rendering
    val realPath: String,  // Real storage path for info dialogs
    val dateModified: Long
)