package com.devson.pixchive.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.pixchive.PixChiveApplication
import com.devson.pixchive.data.ComicFolder
import com.devson.pixchive.data.PreferencesManager
import com.devson.pixchive.utils.FolderScanner
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val imageDao = getApplication<PixChiveApplication>().database.imageDao()

    // ... (Keep layout/sort state flows) ...
    private val _layoutMode = MutableStateFlow("list")
    val layoutMode: StateFlow<String> = _layoutMode.asStateFlow()

    private val _gridColumns = MutableStateFlow(2)
    val gridColumns: StateFlow<Int> = _gridColumns.asStateFlow()

    private val _sortOption = MutableStateFlow("date_newest")
    val sortOption: StateFlow<String> = _sortOption.asStateFlow()

    val folders: StateFlow<List<ComicFolder>> = combine(
        preferencesManager.foldersFlow,
        _sortOption
    ) { folderList, sortOption ->
        sortFolders(folderList, sortOption)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // ... (Keep sortFolders method) ...
    private fun sortFolders(folders: List<ComicFolder>, option: String): List<ComicFolder> {
        return when (option) {
            "name_asc" -> folders.sortedBy { it.displayName.lowercase() }
            "name_desc" -> folders.sortedByDescending { it.displayName.lowercase() }
            "date_newest" -> folders.sortedByDescending { it.dateAdded }
            "date_oldest" -> folders.sortedBy { it.dateAdded }
            else -> folders.sortedByDescending { it.dateAdded }
        }
    }

    fun addFolder(uri: Uri, name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val folderId = UUID.randomUUID().toString()
                val showHidden = preferencesManager.showHiddenFilesFlow.first()

                // Scan and Insert into DB
                FolderScanner.scanAndInsert(
                    uri,
                    folderId,
                    imageDao,
                    showHidden
                )

                // Get counts from DB
                val chapterCount = imageDao.getChapterCount(folderId)
                val imageCount = imageDao.getImageCount(folderId)

                val newFolder = ComicFolder(
                    id = folderId,
                    name = name,
                    uri = uri.toString(),
                    path = uri.path ?: "",
                    chapterCount = chapterCount,
                    imageCount = imageCount,
                    dateAdded = System.currentTimeMillis()
                )

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
            // Clear from Database
            imageDao.deleteFolderContent(folderId)

            // Update Preferences
            val currentFolders = preferencesManager.foldersFlow.first()
            val updatedFolders = currentFolders.filter { it.id != folderId }
            preferencesManager.saveFolders(updatedFolders)
        }
    }

    // ... (Keep setters for layout/sort options) ...
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

    fun clearError() {
        _errorMessage.value = null
    }
}