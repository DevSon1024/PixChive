package com.devson.pixchive.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.devson.pixchive.data.CachedChapter
import com.devson.pixchive.data.CachedFolderData
import com.devson.pixchive.data.CachedImageInfo
import com.devson.pixchive.data.Chapter
import com.devson.pixchive.data.ImageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLDecoder

object FolderScanner {

    private const val TAG = "FolderScanner"

    suspend fun scanFolderWithCache(
        context: Context,
        folderUri: Uri,
        folderId: String,
        forceRescan: Boolean = false
    ): CachedFolderData = withContext(Dispatchers.IO) {
        val cache = com.devson.pixchive.data.FolderCache(context)

        // Return cached data if valid and no force rescan
        if (!forceRescan && cache.isCacheValid(folderId)) {
            cache.getCachedData(folderId)?.let { return@withContext it }
        }

        try {
            // 1. Convert the SAF Uri (content://...) to an absolute path for querying
            // e.g., /storage/emulated/0/Download/Comics
            val rootPath = getAbsolutePathFromSafUri(folderUri)

            if (rootPath == null) {
                Log.e(TAG, "Could not resolve path for URI: $folderUri")
                return@withContext CachedFolderData(folderId, emptyList(), emptyList(), emptyList())
            }

            Log.d(TAG, "Scanning path: $rootPath")

            // 2. Query MediaStore for all images inside this path
            val imageList = queryImagesInFolder(context, rootPath)

            // 3. Group images by their parent folder (Create Chapters)
            val chaptersMap = mutableMapOf<String, MutableList<CachedImageInfo>>()
            val chapterPaths = mutableMapOf<String, String>() // Map Name -> Path

            imageList.forEach { image ->
                val parentPath = File(image.path).parent ?: return@forEach
                val parentName = File(parentPath).name

                // Use the full parent path as the unique key for the chapter
                if (!chaptersMap.containsKey(parentPath)) {
                    chaptersMap[parentPath] = mutableListOf()
                    chapterPaths[parentPath] = parentName
                }
                chaptersMap[parentPath]?.add(image)
            }

            // 4. Build CachedChapters
            val chapters = chaptersMap.map { (path, images) ->
                // Sort images by name within the chapter
                val sortedImages = images.sortedWith(naturalOrderImageComparator())
                val imagePaths = sortedImages.map { it.path }

                CachedChapter(
                    name = chapterPaths[path] ?: "Unknown",
                    path = path,
                    imageCount = images.size,
                    thumbnailPath = sortedImages.firstOrNull()?.path,
                    imagePaths = imagePaths,
                    imageInfo = sortedImages
                )
            }.sortedWith(naturalOrderChapterComparator()) // Sort chapters by name

            // 5. Flatten for "Flat View"
            val allImagePaths = chapters.flatMap { it.imagePaths }
            val allImageInfo = chapters.flatMap { it.imageInfo }

            val cachedData = CachedFolderData(
                folderId = folderId,
                chapters = chapters,
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

    private fun queryImagesInFolder(context: Context, rootPath: String): List<CachedImageInfo> {
        val images = mutableListOf<CachedImageInfo>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA, // Absolute Path
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED
        )

        // Query: Data path starts with rootPath
        val selection = "${MediaStore.Images.Media.DATA} LIKE ?"
        // Add % for wildcard matching subfolders
        val selectionArgs = arrayOf("$rootPath%")

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataCol)
                // Double check ensuring path actually belongs to our root (case sensitivity etc)
                if (path != null && path.startsWith(rootPath)) {
                    images.add(
                        CachedImageInfo(
                            path = path,
                            name = cursor.getString(nameCol) ?: File(path).name,
                            size = cursor.getLong(sizeCol),
                            dateModified = cursor.getLong(dateCol) * 1000 // Convert sec to ms
                        )
                    )
                }
            }
        }
        return images
    }

    /**
     * Converts "content://.../tree/primary:Download/Comics" -> "/storage/emulated/0/Download/Comics"
     */
    private fun getAbsolutePathFromSafUri(uri: Uri): String? {
        try {
            val path = Uri.decode(uri.toString())
            if (path.contains(":")) {
                val id = path.substringAfterLast(":")
                // Primary storage
                if (path.contains("primary") || !path.contains("/tree/")) {
                    return "/storage/emulated/0/$id"
                }
                // SD Card (e.g., 1234-5678:Comics)
                // We need to parse the volume ID. This is a heuristic.
                // A better way usually involves StorageManager, but this works for 90% of cases.
                val volumeId = path.substringAfter("/tree/").substringBefore(":")
                return "/storage/$volumeId/$id"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // --- Converters & Helpers ---

    fun cachedChapterToChapter(context: Context, cached: CachedChapter): Chapter {
        // Map cached info back to live objects
        // Note: We use the stored path directly as the URI for Coil/Image loading
        // because we are now using raw file paths which Coil handles fine with "file://" scheme
        // or implicitly.
        val infoMap = cached.imageInfo.associateBy { it.path }

        val images = cached.imagePaths.map { path ->
            val info = infoMap[path]
            ImageFile(
                name = info?.name ?: File(path).name,
                path = path,
                // Create a File URI (file:///storage/...)
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

    private fun naturalOrderImageComparator(): Comparator<CachedImageInfo> {
        return Comparator { a, b -> compareNatural(a.name, b.name) }
    }

    private fun naturalOrderChapterComparator(): Comparator<CachedChapter> {
        return Comparator { a, b -> compareNatural(a.name, b.name) }
    }

    private fun compareNatural(s1: String, s2: String): Int {
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
                // Compare as numbers
                val cmp = part1.toBigInteger().compareTo(part2.toBigInteger())
                if (cmp != 0) return cmp
            } else {
                // Compare as strings (case insensitive)
                val cmp = part1.compareTo(part2, ignoreCase = true)
                if (cmp != 0) return cmp
            }
        }
        return s1Parts.size - s2Parts.size
    }
}