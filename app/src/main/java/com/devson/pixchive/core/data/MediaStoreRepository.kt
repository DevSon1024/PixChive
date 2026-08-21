package com.devson.pixchive.core.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.devson.pixchive.core.data.models.GalleryFolder
import com.devson.pixchive.core.data.models.GalleryImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreRepository(private val context: Context) {

    // --- PHASE 2 FUNCTION: Gets the folders (fast two-query approach) ---
    suspend fun getFolders(): List<GalleryFolder> = withContext(Dispatchers.IO) {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        return@withContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bucketProjection = arrayOf(
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            )
            val bucketArgs = Bundle().apply {
                putString(
                    ContentResolver.QUERY_ARG_SQL_GROUP_BY,
                    MediaStore.Images.Media.BUCKET_ID
                )
                putString(
                    ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                    "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} ASC"
                )
            }

            val bucketIds = mutableListOf<Pair<String, String>>() // (bucketId, bucketName)
            context.contentResolver.query(uri, bucketProjection, bucketArgs, null)?.use { cursor ->
                val bidCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val bid = cursor.getString(bidCol) ?: continue
                    val name = cursor.getString(nameCol) ?: "Unknown Folder"
                    bucketIds += bid to name
                }
            }

            val resultFolders = mutableListOf<GalleryFolder>()
            for ((bucketId, bucketName) in bucketIds) {
                val detailProjection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.RELATIVE_PATH,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_MODIFIED,
                    MediaStore.Images.Media.SIZE
                )
                val detailArgs = Bundle().apply {
                    putString(
                        ContentResolver.QUERY_ARG_SQL_SELECTION,
                        "${MediaStore.Images.Media.BUCKET_ID} = ?"
                    )
                    putStringArray(
                        ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                        arrayOf(bucketId)
                    )
                    putString(
                        ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                        "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
                    )
                    putInt(ContentResolver.QUERY_ARG_LIMIT, 1)
                }

                var thumbnailUri: Uri? = null
                var folderPath = ""
                var latestDate = 0L

                context.contentResolver.query(uri, detailProjection, detailArgs, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                        thumbnailUri = ContentUris.withAppendedId(uri, id)
                        latestDate = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED))
                        var realPath = c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)) ?: ""
                        val relPathCol = c.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                        val nameColIdx = c.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                        if ((realPath.isBlank() || realPath.startsWith("content://")) &&
                            relPathCol != -1 && nameColIdx != -1
                        ) {
                            val rel = c.getString(relPathCol) ?: ""
                            val name = c.getString(nameColIdx) ?: ""
                            if (rel.isNotBlank() && name.isNotBlank()) {
                                realPath = "/storage/emulated/0/$rel$name"
                            }
                        }
                        folderPath = realPath.substringBeforeLast('/', "")
                    }
                }

                if (thumbnailUri == null) continue

                val statsProjection = arrayOf(MediaStore.Images.Media.SIZE)
                val statsArgs = Bundle().apply {
                    putString(
                        ContentResolver.QUERY_ARG_SQL_SELECTION,
                        "${MediaStore.Images.Media.BUCKET_ID} = ?"
                    )
                    putStringArray(
                        ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                        arrayOf(bucketId)
                    )
                }
                var count = 0
                var totalSize = 0L
                context.contentResolver.query(uri, statsProjection, statsArgs, null)?.use { c ->
                    val sizeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                    while (c.moveToNext()) {
                        count++
                        totalSize += c.getLong(sizeCol)
                    }
                }

                resultFolders += GalleryFolder(
                    bucketId = bucketId,
                    folderName = bucketName,
                    folderPath = folderPath,
                    thumbnailUri = thumbnailUri!!,
                    imageCount = count,
                    size = totalSize,
                    dateModified = latestDate
                )
            }
            resultFolders.sortedBy { it.folderName }
        } else {
            val foldersMap = mutableMapOf<String, GalleryFolder>()
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.DATA
            )
            context.contentResolver.query(
                uri, projection, null, null,
                "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val bidCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val bucketId = cursor.getString(bidCol) ?: continue
                    val bucketName = cursor.getString(nameCol) ?: "Unknown Folder"
                    val size = cursor.getLong(sizeCol)
                    val dateModified = cursor.getLong(dateCol)
                    val realPath = cursor.getString(dataCol) ?: ""
                    val folderPath = realPath.substringBeforeLast('/', "")
                    val contentUri = ContentUris.withAppendedId(uri, id)

                    if (foldersMap.containsKey(bucketId)) {
                        val ex = foldersMap[bucketId]!!
                        foldersMap[bucketId] = ex.copy(imageCount = ex.imageCount + 1, size = ex.size + size)
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
            foldersMap.values.sortedBy { it.folderName }
        }
    }

    suspend fun getFolderName(bucketId: String): String? = withContext(Dispatchers.IO) {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val selection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(bucketId)

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return@withContext cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME))
            }
        }
        return@withContext null
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
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE
        )

        val selection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(bucketId)
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC, ${MediaStore.Images.Media._ID} DESC"

        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val relativePathColumn = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            val displayNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                var realPath = cursor.getString(dataColumn) ?: ""
                
                if ((realPath.isBlank() || realPath.startsWith("content://")) && relativePathColumn != -1 && displayNameColumn != -1) {
                    val relPath = cursor.getString(relativePathColumn) ?: ""
                    val name = cursor.getString(displayNameColumn) ?: ""
                    if (relPath.isNotBlank() && name.isNotBlank()) {
                        realPath = "/storage/emulated/0/$relPath$name"
                    }
                }

                val dateModified = cursor.getLong(dateColumn)
                val size = cursor.getLong(sizeColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                val mimeType = if (mimeTypeColumn != -1) cursor.getString(mimeTypeColumn) ?: "" else ""
                val contentUri = ContentUris.withAppendedId(uri, id)

                imageList.add(
                    GalleryImage(
                        id = id,
                        uri = contentUri,
                        realPath = realPath,
                        dateModified = dateModified,
                        size = size,
                        width = width,
                        height = height,
                        mimeType = mimeType
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
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC"

        context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val relPathCol = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                var realPath = cursor.getString(dataCol) ?: ""
                if ((realPath.isBlank() || realPath.startsWith("content://")) && relPathCol != -1 && nameCol != -1) {
                    val rel = cursor.getString(relPathCol) ?: ""
                    val name = cursor.getString(nameCol) ?: ""
                    if (rel.isNotBlank() && name.isNotBlank()) {
                        realPath = "/storage/emulated/0/$rel$name"
                    }
                }
                val dateAdded = cursor.getLong(dateAddedCol)
                val dateModified = cursor.getLong(dateModCol)
                val size = cursor.getLong(sizeCol)
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)
                val mimeType = if (mimeCol != -1) cursor.getString(mimeCol) ?: "" else ""
                val contentUri = ContentUris.withAppendedId(uri, id)

                imageList.add(GalleryImage(id, contentUri, realPath, dateModified, dateAdded, size, width, height, mimeType))
            }
        }
        return@withContext imageList
    }

    private fun queryMediaStore(
        uri: Uri,
        projection: Array<String>,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String,
        limit: Int,
        offset: Int
    ): android.database.Cursor? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val queryArgs = Bundle().apply {
                putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                if (selection != null) {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                }
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
            }
            context.contentResolver.query(uri, projection, queryArgs, null)
        } else {
            val sortWithLimit = "$sortOrder LIMIT $limit OFFSET $offset"
            context.contentResolver.query(uri, projection, selection, selectionArgs, sortWithLimit)
        }
    }

    suspend fun getAllImagesPaged(
        limit: Int,
        offset: Int,
        sortOrder: String = "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC"
    ): List<GalleryImage> = withContext(Dispatchers.IO) {
        val imageList = mutableListOf<GalleryImage>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE
        )

        queryMediaStore(
            uri = uri,
            projection = projection,
            selection = null,
            selectionArgs = null,
            sortOrder = sortOrder,
            limit = limit,
            offset = offset
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val relPathCol = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                var realPath = cursor.getString(dataCol) ?: ""
                if ((realPath.isBlank() || realPath.startsWith("content://")) && relPathCol != -1 && nameCol != -1) {
                    val rel = cursor.getString(relPathCol) ?: ""
                    val name = cursor.getString(nameCol) ?: ""
                    if (rel.isNotBlank() && name.isNotBlank()) {
                        realPath = "/storage/emulated/0/$rel$name"
                    }
                }
                val dateAdded = cursor.getLong(dateAddedCol)
                val dateModified = cursor.getLong(dateModCol)
                val size = cursor.getLong(sizeCol)
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)
                val mimeType = if (mimeCol != -1) cursor.getString(mimeCol) ?: "" else ""
                val contentUri = ContentUris.withAppendedId(uri, id)

                imageList.add(GalleryImage(id, contentUri, realPath, dateModified, dateAdded, size, width, height, mimeType))
            }
        }
        return@withContext imageList
    }

    suspend fun getImagesForFolderPaged(
        bucketId: String,
        limit: Int,
        offset: Int,
        sortOrder: String = "${MediaStore.Images.Media.DATE_MODIFIED} DESC, ${MediaStore.Images.Media._ID} DESC"
    ): List<GalleryImage> = withContext(Dispatchers.IO) {
        val imageList = mutableListOf<GalleryImage>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE
        )

        val selection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(bucketId)

        queryMediaStore(
            uri = uri,
            projection = projection,
            selection = selection,
            selectionArgs = selectionArgs,
            sortOrder = sortOrder,
            limit = limit,
            offset = offset
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val relativePathColumn = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            val displayNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                var realPath = cursor.getString(dataColumn) ?: ""
                
                if ((realPath.isBlank() || realPath.startsWith("content://")) && relativePathColumn != -1 && displayNameColumn != -1) {
                    val relPath = cursor.getString(relativePathColumn) ?: ""
                    val name = cursor.getString(displayNameColumn) ?: ""
                    if (relPath.isNotBlank() && name.isNotBlank()) {
                        realPath = "/storage/emulated/0/$relPath$name"
                    }
                }

                val dateModified = cursor.getLong(dateColumn)
                val size = cursor.getLong(sizeColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                val mimeType = if (mimeTypeColumn != -1) cursor.getString(mimeTypeColumn) ?: "" else ""
                val contentUri = ContentUris.withAppendedId(uri, id)

                imageList.add(
                    GalleryImage(
                        id = id,
                        uri = contentUri,
                        realPath = realPath,
                        dateModified = dateModified,
                        size = size,
                        width = width,
                        height = height,
                        mimeType = mimeType
                    )
                )
            }
        }
        return@withContext imageList
    }

    suspend fun searchImages(query: String): List<GalleryImage> = withContext(Dispatchers.IO) {
        val imageList = mutableListOf<GalleryImage>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED, MediaStore.Images.Media.SIZE, MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT, MediaStore.Images.Media.RELATIVE_PATH, MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE
        )
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val relPathCol = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                var realPath = cursor.getString(dataCol) ?: ""
                if ((realPath.isBlank() || realPath.startsWith("content://")) && relPathCol != -1 && nameCol != -1) {
                    val rel = cursor.getString(relPathCol) ?: ""
                    val name = cursor.getString(nameCol) ?: ""
                    if (rel.isNotBlank() && name.isNotBlank()) realPath = "/storage/emulated/0/$rel$name"
                }
                val contentUri = ContentUris.withAppendedId(uri, id)
                val mimeType = if (mimeCol != -1) cursor.getString(mimeCol) ?: "" else ""
                imageList.add(GalleryImage(id, contentUri, realPath, cursor.getLong(dateModCol), cursor.getLong(dateAddedCol), cursor.getLong(sizeCol), cursor.getInt(widthCol), cursor.getInt(heightCol), mimeType))
            }
        }
        return@withContext imageList
    }

    suspend fun getSearchSuggestions(query: String): List<GalleryImage> = withContext(Dispatchers.IO) {
        val suggestions = mutableListOf<GalleryImage>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            var count = 0
            while (cursor.moveToNext() && count < 5) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: ""
                val contentUri = ContentUris.withAppendedId(uri, id)
                suggestions.add(GalleryImage(id, contentUri, name, 0, 0, 0, 0, 0))
                count++
            }
        }
        return@withContext suggestions
    }

    suspend fun renameImage(id: Long, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, newName)
            }
            context.contentResolver.update(uri, values, null, null) > 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteImage(id: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            context.contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteImages(ids: List<Long>): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val selection = "${MediaStore.Images.Media._ID} IN (${ids.joinToString(",")})"
            context.contentResolver.delete(uri, selection, null) > 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun renameFolder(folderPath: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val oldFolder = File(folderPath)
            val parent = oldFolder.parentFile
            val newFolder = File(parent, newName)
            
            if (oldFolder.renameTo(newFolder)) {
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(newFolder.absolutePath),
                    null,
                    null
                )
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun observeMediaStoreChanges(): kotlinx.coroutines.flow.Flow<Unit> = kotlinx.coroutines.flow.callbackFlow {
        val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }
}