package com.devson.pixchive.data

import android.net.Uri
import com.devson.pixchive.utils.PathUtils

data class ComicFolder(
    val id: String,
    val name: String,
    val uri: String,
    val path: String,
    val chapterCount: Int = 0,
    val imageCount: Int = 0,
    val dateAdded: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = PathUtils.extractFolderName(name)
}

data class Chapter(
    val name: String,
    val path: String,
    val imageCount: Int,
    val images: List<ImageFile> = emptyList()
) {
    val displayName: String
        get() = PathUtils.extractFolderName(name)
}

data class ImageFile(
    val name: String,
    val path: String,
    val uri: Uri,
    val size: Long = 0,
    val dateModified: Long = 0
)