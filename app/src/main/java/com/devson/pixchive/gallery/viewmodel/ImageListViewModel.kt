package com.devson.pixchive.gallery.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.pixchive.gallery.data.MediaStoreRepository
import com.devson.pixchive.gallery.data.models.GalleryFolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class GalleryState {
    object Loading : GalleryState()
    data class Success(val folders: List<GalleryFolder>) : GalleryState()
    data class Error(val message: String) : GalleryState()
}

// Using AndroidViewModel to easily access context for the MediaStoreRepository
class ImageListViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = MediaStoreRepository(application)

    private val _uiState = MutableStateFlow<GalleryState>(GalleryState.Loading)
    val uiState: StateFlow<GalleryState> = _uiState.asStateFlow()

    init {
        loadGalleryFolders()
    }

    fun loadGalleryFolders() {
        viewModelScope.launch {
            _uiState.value = GalleryState.Loading
            try {
                val folders = repository.getFolders()
                _uiState.value = GalleryState.Success(folders)
            } catch (e: Exception) {
                _uiState.value = GalleryState.Error(e.message ?: "Failed to load device gallery")
            }
        }
    }
}