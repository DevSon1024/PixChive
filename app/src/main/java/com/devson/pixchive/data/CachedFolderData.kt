package com.devson.pixchive.data

data class CachedFolderData(
    val folderId: String,
    val chapters: List<CachedChapter>,
    val allImagePaths: List<String>,
    val allImageInfo: List<CachedImageInfo>,
    val lastScanned: Long = System.currentTimeMillis()
)

data class CachedChapter(
    val name: String,
    val path: String,
    val imageCount: Int,
    val thumbnailPath: String?,
    val imagePaths: List<String>,
    val imageInfo: List<CachedImageInfo>
)

data class CachedImageInfo(
    val path: String,
    val name: String,
    val size: Long,
    val dateModified: Long = 0
)