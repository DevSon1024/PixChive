package com.devson.pixchive.feature.home

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.devson.pixchive.PixChiveApplication
import com.devson.pixchive.core.data.ComicFolder
import com.devson.pixchive.core.data.FolderWithCover
import com.devson.pixchive.core.data.PreferencesManager
import com.devson.pixchive.core.data.local.HistoryEntity
import com.devson.pixchive.core.data.local.ImageEntity
import com.devson.pixchive.core.utils.FolderScanner
import com.devson.pixchive.core.workers.FolderSyncWorker
import com.devson.pixchive.core.workers.FolderValidationWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val app = getApplication<PixChiveApplication>()
    private val imageDao = app.database.imageDao()
    private val historyDao = app.database.historyDao()
    private val favoriteDao = app.database.favoriteDao()

    /** Last 10 chapters the user has read, ordered by last accessed. */
    val recentHistory: StateFlow<List<HistoryEntity>> = historyDao.getRecentHistory()
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Total count of favorited images. */
    val favoriteCount: StateFlow<Int> = favoriteDao.getAllFavoriteUrisFlow()
        .map { it.size }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _layoutMode = MutableStateFlow("list")
    val layoutMode: StateFlow<String> = _layoutMode.asStateFlow()

    private val _gridColumns = MutableStateFlow(2)
    val gridColumns: StateFlow<Int> = _gridColumns.asStateFlow()

    private val _sortOption = MutableStateFlow("date_newest")
    val sortOption: StateFlow<String> = _sortOption.asStateFlow()

    val folders: StateFlow<List<FolderWithCover>> = combine(
        preferencesManager.foldersFlow,
        imageDao.getAllFolderCoversFlow(),
        historyDao.getRecentHistory(),
        _sortOption
    ) { folderList, covers, history, sortOption ->
        val coverMap = covers.associate { it.folderId to it.coverUri }
        val historyMap = history.groupBy { it.folderId }
        val withCovers = folderList.map { folder ->
            val folderHistory = historyMap[folder.id]?.firstOrNull()
            val progress = if (folderHistory != null && folderHistory.totalPages > 0) {
                (folderHistory.currentPage + 1f) / folderHistory.totalPages.toFloat()
            } else 0f
            FolderWithCover(
                folder = folder,
                coverUri = coverMap[folder.id],
                readProgress = progress
            )
        }
        sortFolders(withCovers, sortOption)
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
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val galleryViewMode: StateFlow<String> = preferencesManager.galleryViewModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "albums")

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

    private fun sortFolders(folders: List<FolderWithCover>, option: String): List<FolderWithCover> {
        return when (option) {
            "name_asc" -> folders.sortedBy { it.displayName.lowercase() }
            "name_desc" -> folders.sortedByDescending { it.displayName.lowercase() }
            "date_newest" -> folders.sortedByDescending { it.folder.dateAdded }
            "date_oldest" -> folders.sortedBy { it.folder.dateAdded }
            else -> folders.sortedByDescending { it.folder.dateAdded }
        }
    }

    private fun validateFolders() {
        val workManager = WorkManager.getInstance(getApplication())
        val workRequest = OneTimeWorkRequestBuilder<FolderValidationWorker>().build()
        workManager.enqueueUniqueWork(
            "validate_folders",
            androidx.work.ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    fun refreshFolders() {
        viewModelScope.launch {
            if (_isRefreshing.value) return@launch
            _isRefreshing.value = true

            validateFolders()
            val currentFolders = preferencesManager.foldersFlow.first()
            val workManager = WorkManager.getInstance(getApplication())

            currentFolders.forEach { folder ->
                val showHidden = preferencesManager.showHiddenFilesFlow.first()
                val workRequest = OneTimeWorkRequestBuilder<FolderSyncWorker>()
                    .setInputData(
                        workDataOf(
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

            kotlinx.coroutines.delay(800)
            _isRefreshing.value = false
        }
    }

    fun getLatestImageFlow(folderId: String): Flow<ImageEntity?> {
        return imageDao.getLatestImageFlow(folderId)
    }

    fun addFolder(uri: Uri, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val folderId = UUID.randomUUID().toString()
                val showHidden = preferencesManager.showHiddenFilesFlow.first()

                FolderScanner.scanAndInsert(
                    getApplication(),
                    uri,
                    folderId,
                    imageDao,
                    showHidden
                )

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
        viewModelScope.launch(Dispatchers.IO) {
            imageDao.deleteFolderContent(folderId)
            historyDao.deleteHistoryForFolder(folderId)

            val currentFolders = preferencesManager.foldersFlow.first()
            val updatedFolders = currentFolders.filter { it.id != folderId }
            preferencesManager.saveFolders(updatedFolders)
        }
    }

    fun removeHistoryItem(folderId: String, chapterPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            historyDao.deleteHistory(folderId, chapterPath)
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

    fun clearError() {
        _errorMessage.value = null
    }
}