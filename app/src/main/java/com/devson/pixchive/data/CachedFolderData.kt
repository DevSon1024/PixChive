package com.devson.pixchive.data

data class CachedFolderData(
    val folderId: String,
    val chapters: List<CachedChapter>,
    val allImagePaths: List<String>,
    val lastScanned: Long = System.currentTimeMillis()
)

data class CachedChapter(
    val name: String,
    val path: String,
    val imageCount: Int,
    val thumbnailPath: String?, // First image URI as thumbnail
    val imagePaths: List<String> // Just store paths, not full objects
)
