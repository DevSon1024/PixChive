package com.devson.pixchive.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
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

    companion object {
        private const val TAG = "FolderViewModel"
    }

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
        if (_currentFolder.value?.id == folderId && !forceRescan) {
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            if (_currentFolder.value?.id != folderId) {
                _currentFolder.value = null
                _chapters.value = emptyList()
                _allImages.value = emptyList()
            }

            try {
                val folders = preferencesManager.foldersFlow.first()
                val ignoredPaths = preferencesManager.ignoredPathsFlow.first()
                val showHidden = preferencesManager.showHiddenFilesFlow.first()

                val folder = folders.find { it.id == folderId }

                folder?.let {
                    _currentFolder.value = it
                    val uri = Uri.parse(it.uri)

                    val cachedData = FolderScanner.scanFolderWithCache(
                        getApplication(),
                        uri,
                        folderId,
                        forceRescan,
                        showHidden
                    )

                    // Convert cached data to live objects
                    val allChapters = FolderScanner.cachedDataToChapters(getApplication(), cachedData)

                    // FILTER: Exclude ignored chapters
                    val filteredChapters = allChapters.filter { chapter ->
                        !ignoredPaths.contains(chapter.path)
                    }

                    _chapters.value = filteredChapters

                    // REBUILD allImages from filtered chapters
                    _allImages.value = filteredChapters.flatMap { it.images }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading folder", e)
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeFolder(path: String) {
        viewModelScope.launch {
            // 1. INSTANT UI UPDATE: Remove from current list immediately
            // This ensures the item disappears before the background DB work finishes
            val updatedChapters = _chapters.value.filter { it.path != path }
            _chapters.value = updatedChapters

            // Also update flat view images instantly
            _allImages.value = updatedChapters.flatMap { it.images }

            // 2. Persist to Database (Background)
            preferencesManager.addIgnoredPath(path)
        }
    }

    fun refreshFolder(folderId: String) {
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
        val images = _chapters.value.find { it.path == chapterPath }?.images ?: emptyList()
        return images
    }
}