package com.devson.pixchive.gallery.data.models

import android.net.Uri

data class GalleryFolder(
    val bucketId: String,
    val folderName: String,
    val thumbnailUri: Uri,
    val imageCount: Int,
    val size: Long = 0L,
    val dateModified: Long = 0L
)

data class GalleryImage(
    val id: Long,
    val uri: Uri,
    val realPath: String,
    val dateModified: Long,
    val size: Long = 0L
)