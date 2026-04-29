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
import kotlinx.coroutines.launch

class GalleryFolderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaStoreRepository(application)

    private val _images = MutableStateFlow<List<GalleryImage>>(emptyList())
    val images: StateFlow<List<GalleryImage>> = _images.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val preferencesManager = PreferencesManager(application)
    
    val gridCellsIndex: StateFlow<Int> = preferencesManager.galleryGridCellsIndex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val layoutMode: StateFlow<String> = preferencesManager.galleryLayoutModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "grid")

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

    fun loadImages(bucketId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _images.value = repository.getImagesForFolder(bucketId)
            } catch (e: Exception) {
                _images.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}