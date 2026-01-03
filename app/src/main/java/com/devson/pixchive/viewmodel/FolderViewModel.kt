package com.devson.pixchive.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.pixchive.PixChiveApplication
import com.devson.pixchive.data.Chapter
import com.devson.pixchive.data.ComicFolder
import com.devson.pixchive.data.ImageFile
import com.devson.pixchive.data.PreferencesManager
import com.devson.pixchive.utils.FolderScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

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
                        images = images.map { entity ->
                            ImageFile(
                                name = entity.name,
                                path = entity.path,
                                uri = Uri.fromFile(File(entity.path)),
                                size = entity.size,
                                dateModified = entity.dateModified
                            )
                            // Heavy Sort #1
                        }.sortedWith { a, b -> FolderScanner.compareNatural(a.name, b.name) }
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

    val allImages: StateFlow<List<ImageFile>> = chapters.map { chapterList ->
        chapterList.flatMap { it.images }
    }
        .flowOn(Dispatchers.Default) // Offload this flattening too
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
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

        viewModelScope.launch {
            val folders = preferencesManager.foldersFlow.first()
            val folder = folders.find { it.id == folderId } ?: return@launch

            _currentFolder.value = folder

            val count = imageDao.getImageCount(folderId)
            if (count > 0 && !forceRescan) {
                return@launch
            }

            _isLoading.value = true
            scanJob = launch(Dispatchers.IO) {
                val showHidden = preferencesManager.showHiddenFilesFlow.first()
                val uri = Uri.parse(folder.uri)

                FolderScanner.scanAndInsert(
                    uri,
                    folderId,
                    imageDao,
                    showHidden
                )
                _isLoading.value = false
            }
        }
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