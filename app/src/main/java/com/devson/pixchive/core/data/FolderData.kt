package com.devson.pixchive.core.data
import android.net.Uri
import com.devson.pixchive.core.utils.PathUtils
import com.devson.pixchive.core.data.local.ImageEntity

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

data class FolderWithCover(
    val folder: ComicFolder,
    val coverUri: String? = null,
    val readProgress: Float = 0f
) {
    val id: String get() = folder.id
    val displayName: String get() = folder.displayName
    val chapterCount: Int get() = folder.chapterCount
    val imageCount: Int get() = folder.imageCount
}

data class FolderCover(
    val folderId: String,
    val coverUri: String
)

data class Chapter(
    val name: String,
    val path: String,
    val imageCount: Int,
    val images: List<ImageEntity> = emptyList()
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

data class FolderMetadata(
    val folderName: String = "",
    val coverImageUri: String = "",
    val totalImages: Int = 0,
    val totalSize: Long = 0L,
    val folderSizeFormatted: String = "0 B",
    val lastReadProgress: Float = 0f,
    val lastReadPage: Int = 0,
    val lastReadChapterPath: String? = null
)