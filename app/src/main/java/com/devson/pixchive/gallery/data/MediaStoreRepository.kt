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
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
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

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val bucketId = cursor.getString(bucketIdColumn) ?: continue
                val bucketName = cursor.getString(bucketNameColumn) ?: "Unknown Folder"

                val contentUri = ContentUris.withAppendedId(uri, id)

                if (foldersMap.containsKey(bucketId)) {
                    val existing = foldersMap[bucketId]!!
                    foldersMap[bucketId] = existing.copy(imageCount = existing.imageCount + 1)
                } else {
                    foldersMap[bucketId] = GalleryFolder(
                        bucketId = bucketId,
                        folderName = bucketName,
                        thumbnailUri = contentUri,
                        imageCount = 1
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
            MediaStore.Images.Media.DATE_MODIFIED
        )

        val selection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(bucketId)
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val realPath = cursor.getString(dataColumn) ?: ""
                val dateModified = cursor.getLong(dateColumn)
                val contentUri = ContentUris.withAppendedId(uri, id)

                imageList.add(
                    GalleryImage(
                        id = id,
                        uri = contentUri,
                        realPath = realPath,
                        dateModified = dateModified
                    )
                )
            }
        }
        return@withContext imageList
    }
}