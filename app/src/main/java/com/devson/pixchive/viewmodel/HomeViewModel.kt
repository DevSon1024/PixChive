package com.devson.pixchive.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.pixchive.data.ComicFolder
import com.devson.pixchive.data.PreferencesManager
import com.devson.pixchive.utils.FolderScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)

    private val _folders = MutableStateFlow<List<ComicFolder>>(emptyList())
    val folders: StateFlow<List<ComicFolder>> = _folders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadFolders()
    }

    private fun loadFolders() {
        viewModelScope.launch {
            preferencesManager.foldersFlow.collect { folderList ->
                _folders.value = folderList
            }
        }
    }

    fun addFolder(uri: Uri, name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val folderId = UUID.randomUUID().toString()
                val showHidden = preferencesManager.showHiddenFilesFlow.first()

                // Scan and cache folder
                val cachedData = FolderScanner.scanFolderWithCache(
                    getApplication(),
                    uri,
                    folderId,
                    forceRescan = true,
                    showHidden = showHidden
                )

                val newFolder = ComicFolder(
                    id = folderId,
                    name = name,
                    uri = uri.toString(),
                    path = uri.path ?: "",
                    chapterCount = cachedData.chapters.size,
                    imageCount = cachedData.allImagePaths.size
                )

                val updatedFolders = _folders.value + newFolder
                preferencesManager.saveFolders(updatedFolders)

            } catch (e: Exception) {
                _errorMessage.value = "Failed to add folder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeFolder(folderId: String) {
        viewModelScope.launch {
            val cache = com.devson.pixchive.data.FolderCache(getApplication())
            cache.clearCache(folderId)

            val updatedFolders = _folders.value.filter { it.id != folderId }
            preferencesManager.saveFolders(updatedFolders)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}