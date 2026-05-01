package com.devson.pixchive.gallery.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.pixchive.gallery.data.MediaStoreRepository
import com.devson.pixchive.gallery.data.models.GalleryImage
import com.devson.pixchive.data.PreferencesManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.devson.pixchive.gallery.data.models.GalleryViewSettings

class GalleryFolderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaStoreRepository(application)

    private val _images = MutableStateFlow<List<GalleryImage>>(emptyList())
    private val preferencesManager = PreferencesManager(application)

    val sortOption: StateFlow<String> = preferencesManager.gallerySortOptionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "date_newest")
    private val _currentBucketId = MutableStateFlow("")

    val images: StateFlow<List<GalleryImage>> = combine(_images, sortOption, _currentBucketId) { imgs, sort, bucket ->
        if (bucket == "all_images") imgs else sortImages(imgs, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val gridCellsIndex: StateFlow<Int> = preferencesManager.galleryGridCellsIndex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val layoutMode: StateFlow<String> = preferencesManager.galleryLayoutModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "grid")

    val viewSettings: StateFlow<GalleryViewSettings> = combine(
        preferencesManager.galleryShowThumbnail,
        preferencesManager.galleryShowFileExt,
        preferencesManager.galleryShowResolution,
        preferencesManager.galleryShowPath,
        preferencesManager.galleryShowSize,
        preferencesManager.galleryShowDate
    ) { settingsArray ->
        GalleryViewSettings(
            showThumbnail = settingsArray[0],
            showFileExt = settingsArray[1],
            showResolution = settingsArray[2],
            showPath = settingsArray[3],
            showSize = settingsArray[4],
            showDate = settingsArray[5]
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GalleryViewSettings())

    fun setLayoutMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.setGalleryLayoutMode(mode)
        }
    }

    fun setGridCellsIndex(index: Int) {
        viewModelScope.launch {
            preferencesManager.setGalleryGridCellsIndex(index)
        }
    }

    fun setSortOption(option: String) {
        viewModelScope.launch {
            preferencesManager.setGallerySortOption(option)
        }
    }

    fun updateViewSettings(settings: GalleryViewSettings) {
        viewModelScope.launch {
            preferencesManager.setGalleryShowThumbnail(settings.showThumbnail)
            preferencesManager.setGalleryShowFileExt(settings.showFileExt)
            preferencesManager.setGalleryShowResolution(settings.showResolution)
            preferencesManager.setGalleryShowPath(settings.showPath)
            preferencesManager.setGalleryShowSize(settings.showSize)
            preferencesManager.setGalleryShowDate(settings.showDate)
        }
    }

    fun loadImages(bucketId: String) {
        _currentBucketId.value = bucketId
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (bucketId == "all_images") {
                    _images.value = repository.getAllImages()
                } else {
                    _images.value = repository.getImagesForFolder(bucketId)
                }
            } catch (e: Exception) {
                _images.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun sortImages(images: List<GalleryImage>, sort: String): List<GalleryImage> {
        return when (sort) {
            "name_asc" -> images.sortedBy { it.realPath.substringAfterLast('/').lowercase() }
            "name_desc" -> images.sortedByDescending { it.realPath.substringAfterLast('/').lowercase() }
            "date_newest" -> images.sortedByDescending { it.dateModified }
            "date_oldest" -> images.sortedBy { it.dateModified }
            "size_desc" -> images.sortedByDescending { it.size }
            "size_asc" -> images.sortedBy { it.size }
            "resolution_desc" -> images.sortedByDescending { it.width * it.height }
            "resolution_asc" -> images.sortedBy { it.width * it.height }
            "path_asc" -> images.sortedBy { it.realPath.substringBeforeLast('/').lowercase() }
            "path_desc" -> images.sortedByDescending { it.realPath.substringBeforeLast('/').lowercase() }
            "type_asc" -> images.sortedBy { it.realPath.substringAfterLast('.', "").lowercase() }
            "type_desc" -> images.sortedByDescending { it.realPath.substringAfterLast('.', "").lowercase() }
            else -> images.sortedByDescending { it.dateModified }
        }
    }
}