package com.devson.pixchive.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.devson.pixchive.data.CachedChapter
import com.devson.pixchive.data.CachedFolderData
import com.devson.pixchive.data.CachedImageInfo
import com.devson.pixchive.data.Chapter
import com.devson.pixchive.data.ImageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections

object FolderScanner {

    private const val TAG = "FolderScanner"
    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

    suspend fun scanFolderWithCache(
        context: Context,
        folderUri: Uri,
        folderId: String,
        forceRescan: Boolean = false,
        showHidden: Boolean = false
    ): CachedFolderData = withContext(Dispatchers.IO) {
        val cache = com.devson.pixchive.data.FolderCache(context)

        // Return cached data if valid and no force rescan
        if (!forceRescan && cache.isCacheValid(folderId)) {
            cache.getCachedData(folderId)?.let { return@withContext it }
        }

        try {
            // Convert URI to absolute path
            val rootPath = getAbsolutePathFromSafUri(folderUri)
            if (rootPath == null) {
                Log.e(TAG, "Could not resolve path for URI: $folderUri")
                return@withContext CachedFolderData(folderId, emptyList(), emptyList(), emptyList())
            }

            val rootFile = File(rootPath)
            if (!rootFile.exists() || !rootFile.isDirectory) {
                return@withContext CachedFolderData(folderId, emptyList(), emptyList(), emptyList())
            }

            // Recursive Scan
            val chapters = scanDirectoryRecursive(rootFile, showHidden)

            // Sort chapters
            val sortedChapters = chapters.sortedWith(naturalOrderChapterComparator())

            // Flatten for Flat View
            val allImagePaths = sortedChapters.flatMap { it.imagePaths }
            val allImageInfo = sortedChapters.flatMap { it.imageInfo }

            val cachedData = CachedFolderData(
                folderId = folderId,
                chapters = sortedChapters,
                allImagePaths = allImagePaths,
                allImageInfo = allImageInfo
            )

            // Save to disk
            cache.saveToCache(folderId, cachedData)
            cachedData

        } catch (e: Exception) {
            Log.e(TAG, "Error scanning folder", e)
            e.printStackTrace()
            CachedFolderData(folderId, emptyList(), emptyList(), emptyList())
        }
    }

    private fun scanDirectoryRecursive(
        directory: File,
        showHidden: Boolean
    ): List<CachedChapter> {
        val chapters = mutableListOf<CachedChapter>()
        val currentDirImages = mutableListOf<File>()

        val files = directory.listFiles() ?: return emptyList()

        for (file in files) {
            // Handle Hidden Files filter
            if (!showHidden && file.name.startsWith(".")) {
                continue
            }

            if (file.isDirectory) {
                // Recursion
                chapters.addAll(scanDirectoryRecursive(file, showHidden))
            } else if (file.isFile && isImageFile(file.name)) {
                currentDirImages.add(file)
            }
        }

        // If current directory has images, create a chapter
        if (currentDirImages.isNotEmpty()) {
            // Sort images
            currentDirImages.sortWith { f1, f2 -> compareNatural(f1.name, f2.name) }

            val imagePaths = currentDirImages.map { it.absolutePath }
            val imageInfos = currentDirImages.map { file ->
                CachedImageInfo(
                    path = file.absolutePath,
                    name = file.name,
                    size = file.length(),
                    dateModified = file.lastModified()
                )
            }

            chapters.add(
                CachedChapter(
                    name = directory.name,
                    path = directory.absolutePath,
                    imageCount = imagePaths.size,
                    thumbnailPath = imagePaths.firstOrNull(),
                    imagePaths = imagePaths,
                    imageInfo = imageInfos
                )
            )
        }

        return chapters
    }

    private fun getAbsolutePathFromSafUri(uri: Uri): String? {
        try {
            val path = Uri.decode(uri.toString())
            if (path.contains(":")) {
                val id = path.substringAfterLast(":")
                // Primary storage
                if (path.contains("primary") || !path.contains("/tree/")) {
                    return "/storage/emulated/0/$id"
                }
                // SD Card heuristic
                val volumeId = path.substringAfter("/tree/").substringBefore(":")
                return "/storage/$volumeId/$id"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun isImageFile(filename: String): Boolean {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return extension in imageExtensions
    }

    // --- Converters ---

    fun cachedChapterToChapter(context: Context, cached: CachedChapter): Chapter {
        val infoMap = cached.imageInfo.associateBy { it.path }

        val images = cached.imagePaths.map { path ->
            val info = infoMap[path]
            ImageFile(
                name = info?.name ?: File(path).name,
                path = path,
                uri = Uri.fromFile(File(path)),
                size = info?.size ?: 0L,
                dateModified = info?.dateModified ?: 0L
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
                name = info?.name ?: File(path).name,
                path = path,
                uri = Uri.fromFile(File(path)),
                size = info?.size ?: 0L,
                dateModified = info?.dateModified ?: 0L
            )
        }
    }

    // --- Comparators ---

    private fun naturalOrderChapterComparator(): Comparator<CachedChapter> {
        return Comparator { a, b -> compareNatural(a.name, b.name) }
    }

    // CHANGE: Made public (removed 'private')
    fun compareNatural(s1: String, s2: String): Int {
        val regex = Regex("(\\d+)|(\\D+)")
        val s1Parts = regex.findAll(s1).map { it.value }.toList()
        val s2Parts = regex.findAll(s2).map { it.value }.toList()

        val length = minOf(s1Parts.size, s2Parts.size)
        for (i in 0 until length) {
            val part1 = s1Parts[i]
            val part2 = s2Parts[i]

            val isDigit1 = part1[0].isDigit()
            val isDigit2 = part2[0].isDigit()

            if (isDigit1 && isDigit2) {
                val cmp = part1.toBigInteger().compareTo(part2.toBigInteger())
                if (cmp != 0) return cmp
            } else {
                val cmp = part1.compareTo(part2, ignoreCase = true)
                if (cmp != 0) return cmp
            }
        }
        return s1Parts.size - s2Parts.size
    }
}