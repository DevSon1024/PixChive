package com.devson.pixchive.viewmodel

import android.app.Application
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.pixchive.gallery.data.models.GalleryImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecycleBinViewModel(application: Application) : AndroidViewModel(application) {

    private val _trashedImages = MutableStateFlow<List<GalleryImage>>(emptyList())
    val trashedImages: StateFlow<List<GalleryImage>> = _trashedImages.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadTrashedImages()
    }

    fun loadTrashedImages() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            _isLoading.value = false
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val images = withContext(Dispatchers.IO) {
                    getTrashedImages()
                }
                _trashedImages.value = images
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getTrashedImages(): List<GalleryImage> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        val context = getApplication<Application>()
        val imageList = mutableListOf<GalleryImage>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.IS_TRASHED,
            "date_expires"
        )

        val bundle = android.os.Bundle().apply {
            putInt("android:query-arg-match-trashed", 1)
            putString("android:query-arg-sql-selection", "${MediaStore.Images.Media.IS_TRASHED} = 1")
            putString("android:query-arg-sql-sort-order", "${MediaStore.Images.Media.DATE_MODIFIED} DESC")
        }

        context.contentResolver.query(uri, projection, bundle, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val dateExpiresCol = cursor.getColumnIndex("date_expires")

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val realPath = cursor.getString(dataCol) ?: ""
                val dateModified = cursor.getLong(dateModCol)
                val dateAdded = cursor.getLong(dateAddedCol)
                val size = cursor.getLong(sizeCol)
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)
                val dateExpires = if (dateExpiresCol != -1) cursor.getLong(dateExpiresCol) else 0L
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
        return imageList
    }
}
