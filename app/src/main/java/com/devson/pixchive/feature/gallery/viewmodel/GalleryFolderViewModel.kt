package com.devson.pixchive.feature.gallery.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.devson.pixchive.core.data.MediaStorePagingSource
import com.devson.pixchive.core.data.MediaStoreRepository
import com.devson.pixchive.core.data.PreferencesManager
import com.devson.pixchive.core.data.models.GalleryImage
import com.devson.pixchive.core.data.models.GalleryViewSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Converts a sort-option string (persisted preference) into a MediaStore ORDER BY clause.
 * Only the three supported options (Title, Date, Size) are mapped here.
 * Removed options (resolution, path, type) are redirected to date_newest.
 */
private fun sortOptionToMediaStoreOrder(option: String): String = when (option) {
    "name_asc" -> "${MediaStore.Images.Media.DISPLAY_NAME} ASC"
    "name_desc" -> "${MediaStore.Images.Media.DISPLAY_NAME} DESC"
    "date_oldest" -> "${MediaStore.Images.Media.DATE_MODIFIED} ASC"
    "size_asc" -> "${MediaStore.Images.Media.SIZE} ASC"
    "size_desc" -> "${MediaStore.Images.Media.SIZE} DESC"
    else -> "${MediaStore.Images.Media.DATE_MODIFIED} DESC" // date_newest (default)
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class GalleryFolderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaStoreRepository(application)
    private val preferencesManager = PreferencesManager(application)

    // --- Selection state ---
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    // --- Sort & bucket state ---
    val sortOption: StateFlow<String> = preferencesManager.gallerySortOptionFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "date_newest")

data class AlbumMetadata(
    val folderName: String = "",
    val coverImageUri: Uri? = null,
    val totalImages: Int = 0,
    val totalSizeFormatted: String = ""
)

private val _albumMetadata = MutableStateFlow(AlbumMetadata())
val albumMetadata: StateFlow<AlbumMetadata> = _albumMetadata.asStateFlow()

private val _currentBucketId = MutableStateFlow("")
private val _folderName = MutableStateFlow("Folder Images")
val folderName: StateFlow<String> = _folderName.asStateFlow()

    // --- Active paging source reference (for invalidation on media changes) ---
    private var activePagingSource: MediaStorePagingSource? = null

    /**
     * Paged images driven by the current (bucketId, sortOption) pair.
     * When either changes the pager re-creates a fresh PagingSource via
     * flatMapLatest, which automatically cancels the old one.
     */
    val pagedImages: Flow<PagingData<GalleryImage>> =
        combine(_currentBucketId, sortOption) { bucket, sort -> bucket to sort }
            .flatMapLatest { (bucket, sort) ->
                val msOrder = sortOptionToMediaStoreOrder(sort)
                Pager(
                    config = PagingConfig(
                        pageSize = 90,
                        prefetchDistance = 60,
                        enablePlaceholders = true,
                        initialLoadSize = 90
                    ),
                    pagingSourceFactory = {
                        MediaStorePagingSource(
                            repository = repository,
                            bucketId = bucket.takeIf { it.isNotEmpty() && it != "all_images" },
                            sortOrder = msOrder
                        ).also { activePagingSource = it }
                    }
                ).flow
            }
            .cachedIn(viewModelScope)

    // --- Loading state for the full-screen viewer (ImageViewScreen) ---
    // The paged grid doesn't need a loading flag — pager LoadState handles it.
    // But the HorizontalPager viewer needs a flat list, loaded on demand.
    private val _viewerImages = MutableStateFlow<List<GalleryImage>>(emptyList())
    val images: StateFlow<List<GalleryImage>> = _viewerImages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Called by ImageViewScreen to pre-load all images in a bucket for pager navigation.
     * This is intentionally a flat unbounded query because HorizontalPager needs
     * random access by index. Only called once per bucket open.
     */
    fun loadViewerImages(bucketId: String) {
        if (_viewerImages.value.isNotEmpty()) return // already loaded
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val imgs = when {
                    bucketId == "all_images" -> repository.getAllImages()
                    bucketId.startsWith("search:") -> repository.searchImages(bucketId.removePrefix("search:"))
                    else -> repository.getImagesForFolder(bucketId)
                }
                _viewerImages.value = imgs
            } catch (_: Exception) {
                _viewerImages.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- Preferences-backed UI settings ---
    val gridCellsIndex: StateFlow<Int> = preferencesManager.galleryGridCellsIndex
        .stateIn(viewModelScope, SharingStarted.Lazily, 2)

    val layoutMode: StateFlow<String> = preferencesManager.galleryLayoutModeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "grid")

    val galleryCoverAspectRatio: StateFlow<Float> = preferencesManager.galleryCoverAspectRatioFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 1.0f)

    val isBackgroundBlurEnabled: StateFlow<Boolean> = preferencesManager.isBackgroundBlurEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val viewSettings: StateFlow<GalleryViewSettings> = combine(
        preferencesManager.galleryShowThumbnail,
        preferencesManager.galleryShowFileExt,
        preferencesManager.galleryShowResolution,
        preferencesManager.galleryShowPath,
        preferencesManager.galleryShowSize,
        preferencesManager.galleryShowDate
    ) { arr ->
        GalleryViewSettings(
            showThumbnail = arr[0],
            showFileExt = arr[1],
            showResolution = arr[2],
            showPath = arr[3],
            showSize = arr[4],
            showDate = arr[5]
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, GalleryViewSettings())

    val favorites: StateFlow<Set<String>> = preferencesManager.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    // --- MediaStore change observer ---
    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.observeMediaStoreChanges()
                .debounce(500L)
                .collect {
                    // Invalidate paging source so the grid refreshes without a full reload.
                    activePagingSource?.invalidate()
                }
        }
    }

    
    // Public API
    

    /**
     * Set the folder to display. Updating _currentBucketId triggers a new pager
     * via flatMapLatest — no explicit coroutine needed.
     */
    fun loadImages(bucketId: String, forceRefresh: Boolean = false) {
        if (_currentBucketId.value == bucketId && !forceRefresh) return

        viewModelScope.launch(Dispatchers.IO) {
            val folderDetails = repository.getFolderDetails(bucketId)
            val name = when {
                bucketId == "all_images" -> "All Photos"
                bucketId.startsWith("search:") -> "Search Results"
                folderDetails != null -> folderDetails.folderName
                else -> repository.getFolderName(bucketId) ?: "Folder Images"
            }
            _folderName.value = name

            val totalImages = folderDetails?.imageCount ?: 0
            val totalSize = if (folderDetails != null && folderDetails.size > 0L) {
                com.devson.pixchive.core.utils.FormatUtils.formatFileSize(folderDetails.size)
            } else ""
            val coverUri = folderDetails?.thumbnailUri

            _albumMetadata.value = AlbumMetadata(
                folderName = name,
                coverImageUri = coverUri,
                totalImages = totalImages,
                totalSizeFormatted = totalSize
            )
        }

        _currentBucketId.value = bucketId
    }

    fun setLayoutMode(mode: String) = viewModelScope.launch {
        preferencesManager.setGalleryLayoutMode(mode)
    }

    fun setGalleryCoverAspectRatio(ratio: Float) = viewModelScope.launch {
        preferencesManager.setGalleryCoverAspectRatio(ratio)
    }

    fun setGridCellsIndex(index: Int) = viewModelScope.launch {
        preferencesManager.setGalleryGridCellsIndex(index)
    }

    fun setSortOption(option: String) = viewModelScope.launch {
        preferencesManager.setGallerySortOption(option)
        // flatMapLatest picks up the new sort automatically via sortOption flow.
    }

    fun updateViewSettings(settings: GalleryViewSettings) = viewModelScope.launch {
        preferencesManager.setGalleryShowThumbnail(settings.showThumbnail)
        preferencesManager.setGalleryShowFileExt(settings.showFileExt)
        preferencesManager.setGalleryShowResolution(settings.showResolution)
        preferencesManager.setGalleryShowPath(settings.showPath)
        preferencesManager.setGalleryShowSize(settings.showSize)
        preferencesManager.setGalleryShowDate(settings.showDate)
    }

    fun toggleFavorite(uri: Uri) = viewModelScope.launch {
        preferencesManager.toggleFavorite(uri.toString())
    }

    // --- Selection helpers ---
    fun toggleSelection(id: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun selectAll(images: List<GalleryImage>) {
        _selectedIds.value = images.map { it.id }.toSet()
    }

    fun removeImagesLocally(uris: List<Uri>) {
        // Invalidate so pager re-fetches without the deleted items.
        activePagingSource?.invalidate()
        _selectedIds.value = emptySet()
    }

    fun renameSelectedImage(newName: String, images: List<GalleryImage>) {
        val selectedId = _selectedIds.value.firstOrNull() ?: return
        val image = images.find { it.id == selectedId } ?: return

        viewModelScope.launch {
            if (repository.renameImage(image.id, newName)) {
                activePagingSource?.invalidate()
                clearSelection()
            }
        }
    }
}