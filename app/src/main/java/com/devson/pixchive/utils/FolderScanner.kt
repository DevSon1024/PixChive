package com.devson.pixchive.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.devson.pixchive.data.Chapter
import com.devson.pixchive.data.ImageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

object FolderScanner {

    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

    // Maximum images to scan per folder (prevent hanging on huge folders)
    private const val MAX_IMAGES_PER_CHAPTER = 500

    /**
     * Scan folder and return list of chapters with images (OPTIMIZED)
     */
    suspend fun scanFolder(context: Context, folderUri: Uri): List<Chapter> = withContext(Dispatchers.IO) {
        try {
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()

            // Get all subdirectories (chapters)
            val subFolders = folder.listFiles().filter { it.isDirectory }

            // Scan chapters in parallel for speed
            val chapters = subFolders.map { subFolder ->
                async {
                    try {
                        val images = scanImagesInFolder(subFolder, limit = MAX_IMAGES_PER_CHAPTER)
                        if (images.isNotEmpty()) {
                            Chapter(
                                name = subFolder.name ?: "Unknown",
                                path = subFolder.uri.toString(),
                                imageCount = images.size,
                                images = images
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull()

            // Sort chapters naturally (Chapter 1, Chapter 2, etc.)
            chapters.sortedWith(naturalOrderComparator())
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Scan all images recursively (for Flat View) - LAZY LOADING
     */
    suspend fun scanAllImages(context: Context, folderUri: Uri, limit: Int = 1000): List<ImageFile> =
        withContext(Dispatchers.IO) {
            val allImages = mutableListOf<ImageFile>()

            try {
                val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()
                scanImagesRecursively(folder, allImages, limit)
                allImages.sortedBy { it.name }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    /**
     * Scan images in a specific folder (non-recursive) with limit
     */
    private fun scanImagesInFolder(folder: DocumentFile, limit: Int = MAX_IMAGES_PER_CHAPTER): List<ImageFile> {
        val images = mutableListOf<ImageFile>()

        try {
            var count = 0
            for (file in folder.listFiles()) {
                if (count >= limit) break

                if (file.isFile && isImageFile(file.name ?: "")) {
                    images.add(
                        ImageFile(
                            name = file.name ?: "Unknown",
                            path = file.uri.toString(),
                            uri = file.uri,
                            size = file.length()
                        )
                    )
                    count++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return images.sortedBy { it.name }
    }

    /**
     * Recursively scan all images with limit
     */
    private fun scanImagesRecursively(
        folder: DocumentFile,
        imageList: MutableList<ImageFile>,
        limit: Int
    ) {
        if (imageList.size >= limit) return

        try {
            for (file in folder.listFiles()) {
                if (imageList.size >= limit) break

                when {
                    file.isFile && isImageFile(file.name ?: "") -> {
                        imageList.add(
                            ImageFile(
                                name = file.name ?: "Unknown",
                                path = file.uri.toString(),
                                uri = file.uri,
                                size = file.length()
                            )
                        )
                    }
                    file.isDirectory -> {
                        scanImagesRecursively(file, imageList, limit)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Check if file is an image based on extension
     */
    private fun isImageFile(filename: String): Boolean {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return extension in imageExtensions
    }

    /**
     * Natural order comparator for chapter names
     */
    private fun naturalOrderComparator(): Comparator<Chapter> {
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

    /**
     * Count total images in folder (faster, doesn't load full objects)
     */
    suspend fun countImages(context: Context, folderUri: Uri, limit: Int = 10000): Int =
        withContext(Dispatchers.IO) {
            try {
                val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext 0
                countImagesRecursively(folder, 0, limit)
            } catch (e: Exception) {
                0
            }
        }

    private fun countImagesRecursively(folder: DocumentFile, currentCount: Int, limit: Int): Int {
        if (currentCount >= limit) return currentCount

        var count = currentCount
        try {
            for (file in folder.listFiles()) {
                if (count >= limit) break

                when {
                    file.isFile && isImageFile(file.name ?: "") -> count++
                    file.isDirectory -> count = countImagesRecursively(file, count, limit)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }
}