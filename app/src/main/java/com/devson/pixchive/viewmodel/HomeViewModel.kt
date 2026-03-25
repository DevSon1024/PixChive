package com.devson.pixchive.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.pixchive.PixChiveApplication
import com.devson.pixchive.data.ComicFolder
import com.devson.pixchive.data.PreferencesManager
import com.devson.pixchive.utils.FolderScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.asFlow
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.devson.pixchive.workers.FolderSyncWorker

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val imageDao = getApplication<PixChiveApplication>().database.imageDao()
    private val historyDao = getApplication<PixChiveApplication>().database.historyDao()

    /** Last 10 chapters the user has read, most recent first. */
    val recentHistory = historyDao.getRecentHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val isSyncing: StateFlow<Boolean> = WorkManager.getInstance(application)
        .getWorkInfosByTagLiveData(FolderSyncWorker::class.java.name)
        .asFlow()
        .map { workInfos ->
            workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            _layoutMode.value = preferencesManager.homeLayoutModeFlow.first()
            _sortOption.value = preferencesManager.homeSortOptionFlow.first()
            _gridColumns.value = preferencesManager.homeGridColumnsFlow.first()
            
            validateFolders()
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

    private suspend fun validateFolders() {
        val currentFolders = preferencesManager.foldersFlow.first()
        if (currentFolders.isEmpty()) return

        val validFolders = mutableListOf<ComicFolder>()
        var changed = false
        val contentResolver = getApplication<Application>().contentResolver

        for (folder in currentFolders) {
            val uri = Uri.parse(folder.uri)
            val hasPermission = contentResolver.persistedUriPermissions.any { it.uri == uri }
            
            val exists = try {
                 DocumentFile.fromTreeUri(getApplication(), uri)?.exists() == true
            } catch (e: Exception) { false }
            
            if (hasPermission && exists) {
                validFolders.add(folder)
            } else {
                changed = true
                imageDao.deleteFolderContent(folder.id)
                historyDao.deleteHistoryForFolder(folder.id)
            }
        }
        
        if (changed) {
             preferencesManager.saveFolders(validFolders)
        }
    }

    fun refreshFolders() {
        viewModelScope.launch {
            if (_isRefreshing.value) return@launch
            _isRefreshing.value = true
            
            // Re-validate and trigger sync for all valid folders
            validateFolders()
            val currentFolders = preferencesManager.foldersFlow.first()
            val workManager = WorkManager.getInstance(getApplication())
            
            currentFolders.forEach { folder ->
                val showHidden = preferencesManager.showHiddenFilesFlow.first()
                val workRequest = androidx.work.OneTimeWorkRequestBuilder<FolderSyncWorker>()
                    .setInputData(
                        androidx.work.workDataOf(
                            FolderSyncWorker.KEY_FOLDER_ID to folder.id,
                            FolderSyncWorker.KEY_FOLDER_URI to folder.uri,
                            FolderSyncWorker.KEY_SHOW_HIDDEN to showHidden
                        )
                    )
                    .build()
                    
                workManager.enqueueUniqueWork(
                    "sync_folder_${folder.id}",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            }
            
            // Minimum spinner duration of 1 second for tactical feel
            kotlinx.coroutines.delay(1000)
            _isRefreshing.value = false
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
                    getApplication(),
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

    fun removeHistoryItem(folderId: String, chapterPath: String) {
        viewModelScope.launch {
            historyDao.deleteHistory(folderId, chapterPath)
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