package com.devson.pixchive.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.pixchive.data.ComicFolder
import com.devson.pixchive.data.PreferencesManager
import com.devson.pixchive.utils.FolderScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)

    // Layout Mode (grid/list)
    private val _layoutMode = MutableStateFlow("list")
    val layoutMode: StateFlow<String> = _layoutMode.asStateFlow()

    // Grid Columns
    private val _gridColumns = MutableStateFlow(2)
    val gridColumns: StateFlow<Int> = _gridColumns.asStateFlow()

    // Sort Option
    private val _sortOption = MutableStateFlow("date_newest")
    val sortOption: StateFlow<String> = _sortOption.asStateFlow()

    // Folders are now derived from the data store + sort option
    val folders: StateFlow<List<ComicFolder>> = combine(
        preferencesManager.foldersFlow,
        _sortOption
    ) { folderList, sortOption ->
        sortFolders(folderList, sortOption)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            _layoutMode.value = preferencesManager.homeLayoutModeFlow.first()
            _sortOption.value = preferencesManager.homeSortOptionFlow.first()
            _gridColumns.value = preferencesManager.homeGridColumnsFlow.first()
        }
    }

    private fun sortFolders(folders: List<ComicFolder>, option: String): List<ComicFolder> {
        return when (option) {
            "name_asc" -> folders.sortedBy { it.displayName.lowercase() }
            "name_desc" -> folders.sortedByDescending { it.displayName.lowercase() }
            "date_newest" -> folders.sortedByDescending { it.dateAdded }
            "date_oldest" -> folders.sortedBy { it.dateAdded }
            else -> folders.sortedByDescending { it.dateAdded }
        }
    }

    fun setLayoutMode(mode: String) {
        viewModelScope.launch {
            _layoutMode.value = mode
            preferencesManager.saveHomeLayoutMode(mode)
        }
    }

    fun setSortOption(option: String) {
        viewModelScope.launch {
            _sortOption.value = option
            preferencesManager.saveHomeSortOption(option)
        }
    }

    fun setGridColumns(columns: Int) {
        viewModelScope.launch {
            _gridColumns.value = columns
            preferencesManager.saveHomeGridColumns(columns)
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
                    imageCount = cachedData.allImagePaths.size,
                    dateAdded = System.currentTimeMillis()
                )

                // We get the current raw list from preferences to append
                val currentFolders = preferencesManager.foldersFlow.first()
                val updatedFolders = currentFolders + newFolder
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

            val currentFolders = preferencesManager.foldersFlow.first()
            val updatedFolders = currentFolders.filter { it.id != folderId }
            preferencesManager.saveFolders(updatedFolders)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}