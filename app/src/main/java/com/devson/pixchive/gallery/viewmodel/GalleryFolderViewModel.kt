package com.devson.pixchive.gallery.viewmodel

import android.app.Application
import android.net.Uri
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Dispatchers
import com.devson.pixchive.gallery.data.models.GalleryViewSettings

@OptIn(FlowPreview::class)
class GalleryFolderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaStoreRepository(application)

    private val _images = MutableStateFlow<List<GalleryImage>>(emptyList())
    private val preferencesManager = PreferencesManager(application)

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    val sortOption: StateFlow<String> = preferencesManager.gallerySortOptionFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "date_newest")
    private val _currentBucketId = MutableStateFlow("")
    private val _folderName = MutableStateFlow("Folder Images")
    val folderName: StateFlow<String> = _folderName.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.observeMediaStoreChanges()
                .debounce(500L)
                .collect {
                    val bucketId = _currentBucketId.value
                    if (bucketId.isNotEmpty()) {
                        loadImages(bucketId, forceRefresh = true)
                    }
                }
        }
    }

    val images: StateFlow<List<GalleryImage>> = combine(_images, sortOption, _currentBucketId) { imgs, sort, bucket ->
        if (bucket == "all_images" || bucket.startsWith("search:")) imgs else sortImages(imgs, sort)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val gridCellsIndex: StateFlow<Int> = preferencesManager.galleryGridCellsIndex
        .stateIn(viewModelScope, SharingStarted.Lazily, 2)

    val layoutMode: StateFlow<String> = preferencesManager.galleryLayoutModeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "grid")

    val isBackgroundBlurEnabled: StateFlow<Boolean> = preferencesManager.isBackgroundBlurEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

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
    }.stateIn(viewModelScope, SharingStarted.Lazily, GalleryViewSettings())

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

    val favorites: StateFlow<Set<String>> = preferencesManager.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    fun toggleFavorite(uri: Uri) {
        viewModelScope.launch {
            preferencesManager.toggleFavorite(uri.toString())
        }
    }

    fun loadImages(bucketId: String, forceRefresh: Boolean = false) {
        // If we are already displaying this bucket and not forcing a refresh, do nothing.
        if (_currentBucketId.value == bucketId && !forceRefresh && _images.value.isNotEmpty()) {
            return
        }

        val isNewBucket = _currentBucketId.value != bucketId
        _currentBucketId.value = bucketId

        viewModelScope.launch {
            // Only show loading spinner if it's a new bucket or we currently have no images
            if (isNewBucket || _images.value.isEmpty()) {
                _isLoading.value = true
            }

            try {
                val newImages = when {
                    bucketId == "all_images" -> {
                        _folderName.value = "All Photos"
                        repository.getAllImages()
                    }
                    bucketId.startsWith("search:") -> {
                        _folderName.value = "Search Results"
                        repository.searchImages(bucketId.removePrefix("search:"))
                    }
                    else -> {
                        _folderName.value = repository.getFolderName(bucketId) ?: "Folder Images"
                        repository.getImagesForFolder(bucketId)
                    }
                }
                _images.value = newImages
            } catch (e: Exception) {
                if (isNewBucket) {
                    _images.value = emptyList()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleSelection(id: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun selectAll() {
        _selectedIds.value = _images.value.map { it.id }.toSet()
    }

    fun renameSelectedImage(newName: String) {
        val selectedId = _selectedIds.value.firstOrNull() ?: return
        val image = _images.value.find { it.id == selectedId } ?: return
        val bucketId = _currentBucketId.value

        viewModelScope.launch {
            if (repository.renameImage(image.id, newName)) {
                loadImages(bucketId, forceRefresh = true)
                clearSelection()
            }
        }
    }

    fun removeImagesLocally(uris: List<Uri>) {
        val uriSet = uris.toSet()
        val currentImages = _images.value
        val newImages = currentImages.filter { it.uri !in uriSet }
        _images.value = newImages
        _selectedIds.value = _selectedIds.value.filter { id -> newImages.any { it.id == id } }.toSet()
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