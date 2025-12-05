package com.devson.pixchive.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.devson.pixchive.data.CachedChapter
import com.devson.pixchive.data.CachedFolderData
import com.devson.pixchive.data.CachedImageInfo
import com.devson.pixchive.data.Chapter
import com.devson.pixchive.data.ImageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

object FolderScanner {

    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

    suspend fun scanFolderWithCache(
        context: Context,
        folderUri: Uri,
        folderId: String,
        forceRescan: Boolean = false
    ): CachedFolderData = withContext(Dispatchers.IO) {
        val cache = com.devson.pixchive.data.FolderCache(context)

        if (!forceRescan && cache.isCacheValid(folderId)) {
            cache.getCachedData(folderId)?.let { return@withContext it }
        }

        try {
            val folder = DocumentFile.fromTreeUri(context, folderUri)
                ?: return@withContext CachedFolderData(folderId, emptyList(), emptyList(), emptyList())

            val subFolders = folder.listFiles().filter { it.isDirectory }

            val chapters = subFolders.map { subFolder ->
                async {
                    try {
                        scanChapterFast(context, subFolder)
                    } catch (e: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull()

            val allImagePaths = chapters.flatMap { it.imagePaths }
            val allImageInfo = chapters.flatMap { it.imageInfo }

            val cachedData = CachedFolderData(
                folderId = folderId,
                chapters = chapters.sortedWith(naturalOrderComparator()),
                allImagePaths = allImagePaths,
                allImageInfo = allImageInfo
            )

            cache.saveToCache(folderId, cachedData)
            cachedData
        } catch (e: Exception) {
            e.printStackTrace()
            CachedFolderData(folderId, emptyList(), emptyList(), emptyList())
        }
    }

    private fun scanChapterFast(context: Context, folder: DocumentFile): CachedChapter? {
        try {
            val imagePaths = mutableListOf<String>()
            val imageInfo = mutableListOf<CachedImageInfo>()
            var thumbnailPath: String? = null

            for (file in folder.listFiles()) {
                if (file.isFile && isImageFile(file.name ?: "")) {
                    val path = file.uri.toString()
                    val name = file.name ?: extractCleanFileName(path)
                    val size = file.length()
                    val date = file.lastModified()

                    imagePaths.add(path)
                    imageInfo.add(CachedImageInfo(path, name, size, date))

                    if (thumbnailPath == null) {
                        thumbnailPath = path
                    }
                }
            }

            if (imagePaths.isEmpty()) return null

            return CachedChapter(
                name = folder.name ?: "Unknown",
                path = folder.uri.toString(),
                imageCount = imagePaths.size,
                thumbnailPath = thumbnailPath,
                imagePaths = imagePaths.sorted(),
                imageInfo = imageInfo
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun cachedChapterToChapter(context: Context, cached: CachedChapter): Chapter {
        val infoMap = cached.imageInfo.associateBy { it.path }

        val images = cached.imagePaths.map { path ->
            val info = infoMap[path]
            ImageFile(
                name = info?.name ?: extractCleanFileName(path),
                path = path,
                uri = Uri.parse(path),
                size = info?.size ?: 0L,
                dateModified = info?.dateModified ?: 0L // ✅ Pass Date
            )
        }

        return Chapter(
            name = cached.name,
            path = cached.path,
            imageCount = cached.imageCount,
            images = images
        )
    }

    fun cachedDataToChapters(context: Context, cached: CachedFolderData): List<Chapter> {
        return cached.chapters.map { cachedChapterToChapter(context, it) }
    }

    fun pathsToImageFiles(context: Context, cached: CachedFolderData): List<ImageFile> {
        val infoMap = cached.allImageInfo.associateBy { it.path }

        return cached.allImagePaths.map { path ->
            val info = infoMap[path]
            ImageFile(
                name = info?.name ?: extractCleanFileName(path),
                path = path,
                uri = Uri.parse(path),
                size = info?.size ?: 0L,
                dateModified = info?.dateModified ?: 0L // ✅ Pass Date
            )
        }
    }

    private fun extractCleanFileName(path: String): String {
        return try {
            val decoded = java.net.URLDecoder.decode(path, "UTF-8")
            decoded
                .substringAfterLast('/')
                .substringAfterLast('%')
                .substringAfterLast(':')
        } catch (e: Exception) {
            path.substringAfterLast('/')
        }
    }

    private fun isImageFile(filename: String): Boolean {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return extension in imageExtensions
    }

    private fun naturalOrderComparator(): Comparator<CachedChapter> {
        return Comparator { a, b ->
            val regex = Regex("(\\d+)")
            val aMatch = regex.find(a.name)
            val bMatch = regex.find(b.name)

            when {
                aMatch != null && bMatch != null -> {
                    val aNum = aMatch.value.toIntOrNull() ?: 0
                    val bNum = bMatch.value.toIntOrNull() ?: 0
                    aNum.compareTo(bNum)
                }
                else -> a.name.compareTo(b.name, ignoreCase = true)
            }
        }
    }
}