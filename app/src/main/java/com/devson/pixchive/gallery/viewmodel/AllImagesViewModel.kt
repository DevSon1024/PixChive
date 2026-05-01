package com.devson.pixchive.gallery.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.pixchive.data.PreferencesManager
import com.devson.pixchive.gallery.data.MediaStoreRepository
import com.devson.pixchive.gallery.data.models.GalleryImage
import com.devson.pixchive.gallery.data.models.GalleryViewSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

class AllImagesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaStoreRepository(application)
    private val preferencesManager = PreferencesManager(application)

    private val _uiState = MutableStateFlow<AllImagesState>(AllImagesState.Loading)
    val uiState: StateFlow<AllImagesState> = _uiState.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

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

    init {
        loadAllImages()
    }

    fun loadAllImages() {
        viewModelScope.launch {
            _uiState.value = AllImagesState.Loading
            try {
                val images = repository.getAllImages()
                val (grouped, gridItems) = withContext(Dispatchers.Default) { 
                    val g = groupByDate(images)
                    val items = mutableListOf<Any>()
                    g.forEach { (label, imgs) ->
                        items.add(label)
                        items.addAll(imgs)
                    }
                    g to items
                }
                _uiState.value = AllImagesState.Success(grouped, images, gridItems)
            } catch (e: Exception) {
                _uiState.value = AllImagesState.Error(e.message ?: "Failed to load images")
            }
        }
    }

    fun toggleSelection(id: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
    }

    private var selectionAnchorIndex: Int? = null

    fun selectRange(startIndex: Int, endIndex: Int) {
        val state = _uiState.value as? AllImagesState.Success ?: return
        val items = state.gridItems
        
        val start = minOf(startIndex, endIndex).coerceIn(0, items.lastIndex)
        val end = maxOf(startIndex, endIndex).coerceIn(0, items.lastIndex)
        
        val newSelection = _selectedIds.value.toMutableSet()
        for (i in start..end) {
            val item = items[i]
            if (item is GalleryImage) {
                newSelection.add(item.id)
            }
        }
        _selectedIds.value = newSelection
    }

    fun selectRangeIncremental(startIndex: Int, currentIndex: Int) {
        val state = _uiState.value as? AllImagesState.Success ?: return
        val items = state.gridItems
        
        val start = minOf(startIndex, currentIndex).coerceIn(0, items.lastIndex)
        val end = maxOf(startIndex, currentIndex).coerceIn(0, items.lastIndex)
        
        val currentRangeIds = mutableSetOf<Long>()
        for (i in start..end) {
            val item = items[i]
            if (item is GalleryImage) {
                currentRangeIds.add(item.id)
            }
        }
        
        // When dragging, we want to replace the selection with the range starting from initial anchor
        // But we should keep the items that were already selected before the drag started if we want "additive" behavior.
        // Usually drag-select in galleries is additive to whatever was selected or toggles.
        // For simplicity, let's make it additive for now.
        _selectedIds.value = _selectedIds.value + currentRangeIds
    }

    fun enterSelectionMode(id: Long) {
        _selectedIds.value = setOf(id)
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
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

    private fun groupByDate(images: List<GalleryImage>): Map<String, List<GalleryImage>> {
        val now = Calendar.getInstance()
        val today = calendarMidnight(now)
        val yesterday = calendarMidnight(now).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val sevenDaysAgo = calendarMidnight(now).apply { add(Calendar.DAY_OF_YEAR, -7) }

        val dayOfWeekFmt = SimpleDateFormat("EEEE", Locale.getDefault())
        val olderFmt = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())

        val result = linkedMapOf<String, MutableList<GalleryImage>>()

        for (image in images) {
            val ts = if (image.dateAdded > 0L) image.dateAdded * 1000L else image.dateModified * 1000L
            val imageDate = calendarMidnight(Calendar.getInstance().apply { timeInMillis = ts })

            val key = when {
                !imageDate.before(today) -> "Today"
                !imageDate.before(yesterday) -> "Yesterday"
                !imageDate.before(sevenDaysAgo) -> dayOfWeekFmt.format(Date(ts))
                else -> olderFmt.format(Date(ts))
            }
            result.getOrPut(key) { mutableListOf() }.add(image)
        }
        return result
    }

    private fun calendarMidnight(source: Calendar): Calendar =
        (source.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
}
