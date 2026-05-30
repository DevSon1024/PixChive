package com.devson.pixchive.gallery.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import com.devson.pixchive.data.PreferencesManager
import com.devson.pixchive.gallery.data.MediaStorePagingSource
import com.devson.pixchive.gallery.data.MediaStoreRepository
import com.devson.pixchive.gallery.data.models.GalleryImage
import com.devson.pixchive.gallery.data.models.GalleryViewSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class AllImagesState {
    object Loading : AllImagesState()
    data class Success(
        val grouped: Map<String, List<GalleryImage>>,
        val flatImages: List<GalleryImage>,
        val gridItems: List<Any> = emptyList()
    ) : AllImagesState()
    data class Error(val message: String) : AllImagesState()
}

@OptIn(FlowPreview::class)
class AllImagesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaStoreRepository(application)
    private val preferencesManager = PreferencesManager(application)

    private val _uiState = MutableStateFlow<AllImagesState>(AllImagesState.Loading)
    val uiState: StateFlow<AllImagesState> = _uiState.asStateFlow()

    private val _selectedImagesMap = MutableStateFlow<Map<Long, GalleryImage>>(emptyMap())

    val selectedIds: StateFlow<Set<Long>> = _selectedImagesMap
        .map { it.keys }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val selectedImages: StateFlow<List<GalleryImage>> = _selectedImagesMap
        .map { it.values.toList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private var pagingSource: MediaStorePagingSource? = null

    val pagedGridItems: Flow<PagingData<Any>> = Pager(
        config = PagingConfig(
            pageSize = 60,
            prefetchDistance = 20,
            enablePlaceholders = false
        ),
        pagingSourceFactory = {
            MediaStorePagingSource(repository).also { pagingSource = it }
        }
    ).flow
        .map { pagingData ->
            pagingData.insertSeparators { before: GalleryImage?, after: GalleryImage? ->
                if (after == null) {
                    null
                } else if (before == null) {
                    getDateLabel(after)
                } else {
                    val labelBefore = getDateLabel(before)
                    val labelAfter = getDateLabel(after)
                    if (labelBefore != labelAfter) labelAfter else null
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

    fun setLayoutMode(mode: String) = viewModelScope.launch {
        preferencesManager.setGalleryLayoutMode(mode)
    }

    fun setGridCellsIndex(index: Int) = viewModelScope.launch {
        preferencesManager.setGalleryGridCellsIndex(index)
    }

    fun updateViewSettings(settings: GalleryViewSettings) = viewModelScope.launch {
        preferencesManager.setGalleryShowThumbnail(settings.showThumbnail)
        preferencesManager.setGalleryShowFileExt(settings.showFileExt)
        preferencesManager.setGalleryShowResolution(settings.showResolution)
        preferencesManager.setGalleryShowPath(settings.showPath)
        preferencesManager.setGalleryShowSize(settings.showSize)
        preferencesManager.setGalleryShowDate(settings.showDate)
    }

    val galleryViewMode: StateFlow<String> = preferencesManager.galleryViewModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "all_images")

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
        val imageDate = calendarMidnight(Calendar.getInstance().apply { timeInMillis = ts })
        val now = Calendar.getInstance()
        val today = calendarMidnight(now)
        val yesterday = calendarMidnight(now).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val sevenDaysAgo = calendarMidnight(now).apply { add(Calendar.DAY_OF_YEAR, -7) }

        val dayOfWeekFmt = SimpleDateFormat("EEEE", Locale.getDefault())
        val olderFmt = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())

        return when {
            !imageDate.before(today) -> "Today"
            !imageDate.before(yesterday) -> "Yesterday"
            !imageDate.before(sevenDaysAgo) -> dayOfWeekFmt.format(Date(ts))
            else -> olderFmt.format(Date(ts))
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
