package com.devson.pixchive.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.pixchive.PixChiveApplication
import com.devson.pixchive.data.Chapter
import com.devson.pixchive.data.ComicFolder
import com.devson.pixchive.data.PreferencesManager
import com.devson.pixchive.utils.FolderScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import java.io.File
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.work.ExistingWorkPolicy
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.devson.pixchive.data.local.ImageEntity
import com.devson.pixchive.workers.FolderSyncWorker

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class FolderViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val imageDao = getApplication<PixChiveApplication>().database.imageDao()

    private val _currentFolder = MutableStateFlow<ComicFolder?>(null)
    val currentFolder: StateFlow<ComicFolder?> = _currentFolder.asStateFlow()

    private val _sortOption = MutableStateFlow("name_asc")
    val sortOption: StateFlow<String> = _sortOption.asStateFlow()

    // --- MAIN DATA FLOW ---
    val chapters: StateFlow<List<Chapter>> = combine(
        _currentFolder.filterNotNull(),
        _sortOption,
        preferencesManager.ignoredPathsFlow
    ) { folder, sort, ignoredPaths ->
        // Observe DB
        imageDao.getImagesFlow(folder.id)
            // 1. DEBOUNCE: Wait 250ms for burst inserts to settle before processing
            // This drastically reduces how often we re-sort the list during scanning
            .debounce(250)
            .map { entities ->
                // 2. HEAVY COMPUTATION: Grouping and sorting
                val grouped = entities.groupBy { it.parentFolderPath }
                    .filter { (path, _) -> !ignoredPaths.contains(path) }

                val chapterList = grouped.map { (path, images) ->
                    Chapter(
                        name = images.first().parentFolderName,
                        path = path,
                        imageCount = images.size,
                        images = images.sortedWith { a, b -> FolderScanner.compareNatural(a.name, b.name) }
                    )
                }
                // Heavy Sort #2
                sortChapters(chapterList, sort)
            }
            // 3. OFFLOAD: Move all the map/sort logic above to the Default (CPU) dispatcher
            // This frees up the Main Thread to handle touch events, preventing ANR.
            .flowOn(Dispatchers.Default)
    }.flatMapLatest { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- PAGING 3 FOR FLAT VIEW ---
    val flatImages: Flow<PagingData<ImageEntity>> = _currentFolder.filterNotNull()
        .flatMapLatest { folder ->
            Pager(
                config = PagingConfig(
                    pageSize = 60,
                    enablePlaceholders = true,
                    maxSize = 200, // keep memory footprint low
                    prefetchDistance = 10
                ),
                pagingSourceFactory = { imageDao.getImagesByFolderPaged(folder.id) }
            ).flow
        }.cachedIn(viewModelScope)

    // Total count of images in the flat view — used by ReaderScreen for unbounded paging
    val flatImageCount: StateFlow<Int> = _currentFolder
        .filterNotNull()
        .flatMapLatest { folder -> imageDao.getImageCountFlow(folder.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Loads a single image by its sorted position in the flat view (for on-demand reader page). */
    suspend fun getFlatImageAt(index: Int): ImageEntity? {
        val folderId = _currentFolder.value?.id ?: return null
        return imageDao.getImageByIndex(folderId, index)
    }

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _viewMode = MutableStateFlow("explorer")
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    private val _layoutMode = MutableStateFlow("grid")
    val layoutMode: StateFlow<String> = _layoutMode.asStateFlow()

    private val _gridColumns = MutableStateFlow(3)
    val gridColumns: StateFlow<Int> = _gridColumns.asStateFlow()

    private var scanJob: Job? = null

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            _viewMode.value = preferencesManager.viewModeFlow.first()
            _layoutMode.value = preferencesManager.layoutModeFlow.first()
            _gridColumns.value = preferencesManager.folderGridColumnsFlow.first()
            _sortOption.value = preferencesManager.folderSortOptionFlow.first()
        }
    }

    private fun sortChapters(chapters: List<Chapter>, option: String): List<Chapter> {
        val comparator = Comparator<Chapter> { a, b ->
            FolderScanner.compareNatural(a.name, b.name)
        }
        return when (option) {
            "name_asc" -> chapters.sortedWith(comparator)
            "name_desc" -> chapters.sortedWith(comparator).reversed()
            else -> chapters.sortedWith(comparator)
        }
    }

    fun loadFolder(folderId: String, forceRescan: Boolean = false) {
        if (folderId == "favorites") return

        scanJob?.cancel()
        _isLoading.value = true

        viewModelScope.launch {
            val folders = preferencesManager.foldersFlow.first()
            val folder = folders.find { it.id == folderId }

            if (folder != null) {
                _currentFolder.value = folder
            } else {
                _isLoading.value = false
                return@launch
            }

            val count = imageDao.getImageCount(folderId)
            if (count > 0 && !forceRescan) {
                // Wait for the chapters flow to emit real data before hiding the loader.
                // 3-second timeout is a safety net for genuinely empty folders.
                withTimeoutOrNull(3_000) {
                    chapters.first { it.isNotEmpty() }
                }
                _isLoading.value = false
                return@launch
            }

            val showHidden = preferencesManager.showHiddenFilesFlow.first()
            val uri = Uri.parse(folder.uri)
            enqueueSyncWorker(folderId, uri.toString(), showHidden)
            _isLoading.value = false
        }
    }

    private fun enqueueSyncWorker(folderId: String, folderUri: String, showHidden: Boolean) {
        val workManager = WorkManager.getInstance(getApplication())
        
        val workRequest = OneTimeWorkRequestBuilder<FolderSyncWorker>()
            .setInputData(
                workDataOf(
                    FolderSyncWorker.KEY_FOLDER_ID to folderId,
                    FolderSyncWorker.KEY_FOLDER_URI to folderUri,
                    FolderSyncWorker.KEY_SHOW_HIDDEN to showHidden
                )
            )
            .build()
            
        // Use KEEP policy so if it's already scanning, we let it finish
        workManager.enqueueUniqueWork(
            "sync_folder_$folderId",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    fun refreshFolder(folderId: String) {
        if (folderId != "favorites") {
            loadFolder(folderId, forceRescan = true)
        }
    }

    fun removeFolder(path: String) {
        viewModelScope.launch {
            preferencesManager.addIgnoredPath(path)
        }
    }

    fun setViewMode(mode: String) {
        viewModelScope.launch {
            _viewMode.value = mode
            preferencesManager.saveViewMode(mode)
        }
    }

    fun setLayoutMode(mode: String) {
        viewModelScope.launch {
            _layoutMode.value = mode
            preferencesManager.saveLayoutMode(mode)
        }
    }

    fun setGridColumns(columns: Int) {
        viewModelScope.launch {
            _gridColumns.value = columns
            preferencesManager.saveFolderGridColumns(columns)
        }
    }

    fun setSortOption(option: String) {
        viewModelScope.launch {
            _sortOption.value = option
            preferencesManager.saveFolderSortOption(option)
        }
    }
}