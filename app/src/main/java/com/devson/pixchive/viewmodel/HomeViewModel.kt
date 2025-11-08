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
                // Scan folder to count chapters and images
                val chapters = FolderScanner.scanFolder(getApplication(), uri)
                val imageCount = FolderScanner.countImages(getApplication(), uri)

                val newFolder = ComicFolder(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    uri = uri.toString(),
                    path = uri.path ?: "",
                    chapterCount = chapters.size,
                    imageCount = imageCount
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
            val updatedFolders = _folders.value.filter { it.id != folderId }
            preferencesManager.saveFolders(updatedFolders)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}