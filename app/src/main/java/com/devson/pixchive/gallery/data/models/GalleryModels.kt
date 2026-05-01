package com.devson.pixchive.gallery.data.models

import android.net.Uri

data class GalleryFolder(
    val bucketId: String,
    val folderName: String,
    val folderPath: String,
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
    val dateAdded: Long = 0L,
    val size: Long = 0L,
    val width: Int = 0,
    val height: Int = 0
)

data class GalleryViewSettings(
    val showThumbnail: Boolean = true,
    val showFileExt: Boolean = true,
    val showResolution: Boolean = true,
    val showPath: Boolean = false,
    val showSize: Boolean = true,
    val showDate: Boolean = true
)