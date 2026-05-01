package com.devson.pixchive.gallery.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.pixchive.gallery.data.MediaStoreRepository
import com.devson.pixchive.gallery.data.models.GalleryFolder
import com.devson.pixchive.data.PreferencesManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.devson.pixchive.gallery.data.models.GalleryViewSettings

sealed class GalleryState {
    object Loading : GalleryState()
    data class Success(val folders: List<GalleryFolder>) : GalleryState()
    data class Error(val message: String) : GalleryState()
}

// Using AndroidViewModel to easily access context for the MediaStoreRepository
class ImageListViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = MediaStoreRepository(application)

    private val _uiState = MutableStateFlow<GalleryState>(GalleryState.Loading)
    private val _folders = MutableStateFlow<List<GalleryFolder>>(emptyList())
    
    private val preferencesManager = PreferencesManager(application)
    
    val sortOption: StateFlow<String> = preferencesManager.gallerySortOptionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "name_asc")

    val uiState: StateFlow<GalleryState> = combine(_uiState, sortOption, _folders) { state, sort, folders ->
        if (state is GalleryState.Success) {
            GalleryState.Success(sortFolders(folders, sort))
        } else {
            state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GalleryState.Loading)

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

    init {
        loadGalleryFolders()
    }

    fun loadGalleryFolders() {
        viewModelScope.launch {
            _uiState.value = GalleryState.Loading
            try {
                val folders = repository.getFolders()
                _folders.value = folders
                _uiState.value = GalleryState.Success(folders)
            } catch (e: Exception) {
                _uiState.value = GalleryState.Error(e.message ?: "Failed to load device gallery")
            }
        }
    }

    private fun sortFolders(folders: List<GalleryFolder>, sort: String): List<GalleryFolder> {
        return when (sort) {
            "name_asc" -> folders.sortedBy { it.folderName.lowercase() }
            "name_desc" -> folders.sortedByDescending { it.folderName.lowercase() }
            "date_newest" -> folders.sortedByDescending { it.dateModified }
            "date_oldest" -> folders.sortedBy { it.dateModified }
            "size_desc" -> folders.sortedByDescending { it.size }
            "size_asc" -> folders.sortedBy { it.size }
            "path_asc" -> folders.sortedBy { it.folderPath.lowercase() }
            "path_desc" -> folders.sortedByDescending { it.folderPath.lowercase() }
            else -> folders.sortedBy { it.folderName.lowercase() }
        }
    }
}