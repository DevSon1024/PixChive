package com.devson.pixchive.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.pixchive.data.Chapter
import com.devson.pixchive.data.ComicFolder
import com.devson.pixchive.data.FolderCache
import com.devson.pixchive.data.ImageFile
import com.devson.pixchive.data.PreferencesManager
import com.devson.pixchive.utils.FolderScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FolderViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val folderCache = FolderCache(application)

    private val _currentFolder = MutableStateFlow<ComicFolder?>(null)
    val currentFolder: StateFlow<ComicFolder?> = _currentFolder.asStateFlow()

    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters.asStateFlow()

    private val _allImages = MutableStateFlow<List<ImageFile>>(emptyList())
    val allImages: StateFlow<List<ImageFile>> = _allImages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _viewMode = MutableStateFlow("explorer")
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    private val _layoutMode = MutableStateFlow("grid")
    val layoutMode: StateFlow<String> = _layoutMode.asStateFlow()

    // Track loaded folders to prevent re-scanning
    private val loadedFolders = mutableSetOf<String>()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            _viewMode.value = preferencesManager.viewModeFlow.first()
            _layoutMode.value = preferencesManager.layoutModeFlow.first()
        }
    }

    fun loadFolder(folderId: String, forceRescan: Boolean = false) {
        // Skip if already loaded and not forcing rescan
        if (loadedFolders.contains(folderId) && !forceRescan) {
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            try {
                val folders = preferencesManager.foldersFlow.first()
                val folder = folders.find { it.id == folderId }

                folder?.let {
                    _currentFolder.value = it
                    val uri = Uri.parse(it.uri)

                    // Load from cache or scan
                    val cachedData = FolderScanner.scanFolderWithCache(
                        getApplication(),
                        uri,
                        folderId,
                        forceRescan
                    )

                    // Convert cached data to live objects
                    _chapters.value = FolderScanner.cachedDataToChapters(cachedData)
                    _allImages.value = FolderScanner.pathsToImageFiles(cachedData.allImagePaths)

                    // Mark as loaded
                    loadedFolders.add(folderId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshFolder(folderId: String) {
        loadedFolders.remove(folderId)
        folderCache.clearCache(folderId)
        loadFolder(folderId, forceRescan = true)
    }

    fun setViewMode(mode: String) {
        viewModelScope.launch {
            _viewMode.value = mode
            preferencesManager.saveViewMode(mode)
        }
    }

    fun toggleLayoutMode() {
        viewModelScope.launch {
            val newMode = if (_layoutMode.value == "grid") "list" else "grid"
            _layoutMode.value = newMode
            preferencesManager.saveLayoutMode(newMode)
        }
    }

    fun getChapterImages(chapterPath: String): List<ImageFile> {
        return _chapters.value.find { it.path == chapterPath }?.images ?: emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        loadedFolders.clear()
    }
}
