package com.devson.pixchive.gallery.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.devson.pixchive.gallery.data.models.GalleryFolder
import com.devson.pixchive.gallery.data.models.GalleryImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreRepository(private val context: Context) {

    // --- PHASE 2 FUNCTION: Gets the folders ---
    suspend fun getFolders(): List<GalleryFolder> = withContext(Dispatchers.IO) {
        val foldersMap = mutableMapOf<String, GalleryFolder>()

        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DATA
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val bucketId = cursor.getString(bucketIdColumn) ?: continue
                val bucketName = cursor.getString(bucketNameColumn) ?: "Unknown Folder"
                val size = cursor.getLong(sizeColumn)
                val dateModified = cursor.getLong(dateModifiedColumn)
                val realPath = cursor.getString(dataColumn) ?: ""
                val folderPath = realPath.substringBeforeLast('/', "")

                val contentUri = ContentUris.withAppendedId(uri, id)

                if (foldersMap.containsKey(bucketId)) {
                    val existing = foldersMap[bucketId]!!
                    foldersMap[bucketId] = existing.copy(
                        imageCount = existing.imageCount + 1,
                        size = existing.size + size
                    )
                } else {
                    foldersMap[bucketId] = GalleryFolder(
                        bucketId = bucketId,
                        folderName = bucketName,
                        folderPath = folderPath,
                        thumbnailUri = contentUri,
                        imageCount = 1,
                        size = size,
                        dateModified = dateModified
                    )
                }
            }
        }

        return@withContext foldersMap.values.sortedBy { it.folderName }
    }
    suspend fun getImagesForFolder(bucketId: String): List<GalleryImage> = withContext(Dispatchers.IO) {
        val imageList = mutableListOf<GalleryImage>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        val selection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(bucketId)
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val realPath = cursor.getString(dataColumn) ?: ""
                val dateModified = cursor.getLong(dateColumn)
                val size = cursor.getLong(sizeColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                val contentUri = ContentUris.withAppendedId(uri, id)

                imageList.add(
                    GalleryImage(
                        id = id,
                        uri = contentUri,
                        realPath = realPath,
                        dateModified = dateModified,
                        size = size,
                        width = width,
                        height = height
                    )
                )
            }
        }
        return@withContext imageList
    }

    suspend fun getAllImages(): List<GalleryImage> = withContext(Dispatchers.IO) {
        val imageList = mutableListOf<GalleryImage>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val realPath = cursor.getString(dataCol) ?: ""
                val dateAdded = cursor.getLong(dateAddedCol)
                val dateModified = cursor.getLong(dateModCol)
                val size = cursor.getLong(sizeCol)
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)
                val contentUri = ContentUris.withAppendedId(uri, id)

                imageList.add(
                    GalleryImage(
                        id = id,
                        uri = contentUri,
                        realPath = realPath,
                        dateModified = dateModified,
                        dateAdded = dateAdded,
                        size = size,
                        width = width,
                        height = height
                    )
                )
            }
        }
        return@withContext imageList
    }
}