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
import androidx.paging.insertSeparators
import androidx.paging.map
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Sealed class representing UI items in the gallery list or grid:
 * either a sticky date header separator or a media photo item.
 */
sealed class GalleryUiModel {
    data class DateHeaderItem(val label: String, val id: String) : GalleryUiModel()
    data class MediaItem(val image: GalleryImage) : GalleryUiModel()
}

// Backward compatibility alias for any existing references
typealias GalleryItem = GalleryUiModel

sealed class AllImagesState {
    object Loading : AllImagesState()
    data class Success(
        val grouped: Map<String, List<GalleryImage>>,
        val flatImages: List<GalleryImage>,
        val gridItems: List<Any> = emptyList()
    ) : AllImagesState()
    data class Error(val message: String) : AllImagesState()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AllImagesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaStoreRepository(application)
    private val preferencesManager = PreferencesManager(application)

    private val _uiState = MutableStateFlow<AllImagesState>(AllImagesState.Loading)
    val uiState: StateFlow<AllImagesState> = _uiState.asStateFlow()

    // --- Selection State ---
    private val _selectedImagesMap = MutableStateFlow<Map<Long, GalleryImage>>(emptyMap())

    val selectedIds: StateFlow<Set<Long>> = _selectedImagesMap
        .map { it.keys }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val selectedImages: StateFlow<List<GalleryImage>> = _selectedImagesMap
        .map { it.values.toList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Gallery Preferences StateFlows ---
    val galleryCoverAspectRatio: StateFlow<Float> = preferencesManager.galleryCoverAspectRatioFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val galleryGridColumns: StateFlow<Int> = preferencesManager.galleryGridColumnsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    val isGalleryListMode: StateFlow<Boolean> = preferencesManager.isGalleryListModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val layoutMode: StateFlow<String> = preferencesManager.galleryLayoutModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "grid")

    val gridCellsIndex: StateFlow<Int> = preferencesManager.galleryGridCellsIndex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GalleryViewSettings())

    val sortOption: StateFlow<String> = preferencesManager.gallerySortOptionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "date_newest")

    val sortOrderAscending: StateFlow<Boolean> = sortOption
        .map { it == "date_oldest" || it == "date_asc" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val galleryViewMode: StateFlow<String> = preferencesManager.galleryViewModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "all_images")

    private var pagingSource: MediaStorePagingSource? = null

    // --- Paging 3 Flow with Date Separators mapping to GalleryUiModel ---
    val pagedGridItems: Flow<PagingData<GalleryUiModel>> = sortOption
        .flatMapLatest { sort ->
            val isAsc = (sort == "date_oldest" || sort == "date_asc")
            val msOrder = if (isAsc) {
                "${MediaStore.Images.Media.DATE_ADDED} ASC, ${MediaStore.Images.Media._ID} ASC"
            } else {
                "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC"
            }
            Pager(
                config = PagingConfig(
                    pageSize = 90,
                    prefetchDistance = 60,
                    enablePlaceholders = true,
                    initialLoadSize = 90
                ),
                pagingSourceFactory = {
                    MediaStorePagingSource(repository, sortOrder = msOrder).also { pagingSource = it }
                }
            ).flow
                .map { pagingData: PagingData<GalleryImage> ->
                    val mediaItemData: PagingData<GalleryUiModel> = pagingData.map { GalleryUiModel.MediaItem(it) }
                    mediaItemData.insertSeparators { before: GalleryUiModel?, after: GalleryUiModel? ->
                        val beforeImg = (before as? GalleryUiModel.MediaItem)?.image
                        val afterImg = (after as? GalleryUiModel.MediaItem)?.image

                        if (afterImg == null) {
                            null
                        } else if (beforeImg == null) {
                            val label = getDateLabel(afterImg)
                            GalleryUiModel.DateHeaderItem(label = label, id = "header_${label}_${afterImg.id}")
                        } else {
                            val labelBefore = getDateLabel(beforeImg)
                            val labelAfter = getDateLabel(afterImg)
                            if (labelBefore != labelAfter) {
                                GalleryUiModel.DateHeaderItem(label = labelAfter, id = "header_${labelAfter}_${afterImg.id}")
                            } else {
                                null
                            }
                        }
                    }
                }
        }
        .cachedIn(viewModelScope)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.observeMediaStoreChanges()
                .debounce(500L)
                .collect {
                    refresh()
                }
        }
    }

    fun refresh() {
        pagingSource?.invalidate()
    }

    // --- Selection Operations ---
    fun toggleSelection(image: GalleryImage) {
        val current = _selectedImagesMap.value.toMutableMap()
        if (current.containsKey(image.id)) {
            current.remove(image.id)
        } else {
            current[image.id] = image
        }
        _selectedImagesMap.value = current
    }

    fun clearSelection() {
        _selectedImagesMap.value = emptyMap()
    }

    fun selectAll() {
        viewModelScope.launch {
            try {
                val allImages = repository.getAllImages()
                _selectedImagesMap.value = allImages.associateBy { it.id }
            } catch (e: Exception) {
                // Ignore failure
            }
        }
    }

    // --- Preference Setters ---
    fun setGalleryCoverAspectRatio(ratio: Float) = viewModelScope.launch {
        preferencesManager.setGalleryCoverAspectRatio(ratio)
    }

    fun setGalleryGridColumns(columns: Int) = viewModelScope.launch {
        preferencesManager.setGalleryGridColumns(columns)
    }

    fun setGalleryListMode(isList: Boolean) = viewModelScope.launch {
        preferencesManager.setGalleryListMode(isList)
    }

    fun toggleGalleryListMode() = viewModelScope.launch {
        preferencesManager.setGalleryListMode(!isGalleryListMode.value)
    }

    fun setLayoutMode(mode: String) = viewModelScope.launch {
        preferencesManager.setGalleryLayoutMode(mode)
    }

    fun setGridCellsIndex(index: Int) = viewModelScope.launch {
        preferencesManager.setGalleryGridCellsIndex(index)
    }

    fun setSortOption(option: String) = viewModelScope.launch {
        preferencesManager.setGallerySortOption(option)
    }

    fun toggleSortOrder() = viewModelScope.launch {
        val currentIsAsc = sortOption.value == "date_oldest" || sortOption.value == "date_asc"
        val newOption = if (currentIsAsc) "date_newest" else "date_oldest"
        preferencesManager.setGallerySortOption(newOption)
        refresh()
    }

    fun setSortOrderAscending(ascending: Boolean) = viewModelScope.launch {
        val newOption = if (ascending) "date_oldest" else "date_newest"
        preferencesManager.setGallerySortOption(newOption)
        refresh()
    }

    fun updateViewSettings(settings: GalleryViewSettings) = viewModelScope.launch {
        preferencesManager.setGalleryShowThumbnail(settings.showThumbnail)
        preferencesManager.setGalleryShowFileExt(settings.showFileExt)
        preferencesManager.setGalleryShowResolution(settings.showResolution)
        preferencesManager.setGalleryShowPath(settings.showPath)
        preferencesManager.setGalleryShowSize(settings.showSize)
        preferencesManager.setGalleryShowDate(settings.showDate)
    }

    fun setGalleryViewMode(mode: String) = viewModelScope.launch {
        preferencesManager.setGalleryViewMode(mode)
    }

    fun renameSelectedImage(newName: String) {
        val selectedId = selectedIds.value.firstOrNull() ?: return
        val image = _selectedImagesMap.value[selectedId] ?: return

        viewModelScope.launch {
            if (repository.renameImage(image.id, newName)) {
                refresh()
                clearSelection()
            }
        }
    }

    fun removeImagesLocally(uris: List<Uri>) {
        refresh()
    }

    private fun getDateLabel(image: GalleryImage): String {
        val ts = if (image.dateAdded > 0L) image.dateAdded * 1000L else image.dateModified * 1000L
        if (ts <= 0L) return "Undated"
        val imageCal = Calendar.getInstance().apply { timeInMillis = ts }
        val imageDate = calendarMidnight(imageCal)
        val now = Calendar.getInstance()
        val today = calendarMidnight(now)
        val yesterday = calendarMidnight(now).apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            !imageDate.before(today) -> "Today"
            !imageDate.before(yesterday) -> "Yesterday"
            now.get(Calendar.YEAR) == imageCal.get(Calendar.YEAR) -> {
                SimpleDateFormat("MMMM d", Locale.getDefault()).format(Date(ts))
            }
            else -> {
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(ts))
            }
        }
    }

    private fun calendarMidnight(source: Calendar): Calendar =
        (source.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
}