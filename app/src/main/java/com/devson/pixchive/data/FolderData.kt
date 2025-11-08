package com.devson.pixchive.data

import android.net.Uri

data class ComicFolder(
    val id: String,
    val name: String,
    val uri: String,
    val path: String,
    val chapterCount: Int = 0,
    val imageCount: Int = 0,
    val dateAdded: Long = System.currentTimeMillis()
)

data class Chapter(
    val name: String,
    val path: String,
    val imageCount: Int,
    val images: List<ImageFile> = emptyList()
)

data class ImageFile(
    val name: String,
    val path: String,
    val uri: Uri,
    val size: Long = 0
)
