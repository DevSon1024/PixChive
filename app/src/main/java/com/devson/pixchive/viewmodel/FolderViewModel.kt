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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FolderViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val folderCache = FolderCache(application)

    private val _currentFolder = MutableStateFlow<ComicFolder?>(null)
    val currentFolder: StateFlow<ComicFolder?> = _currentFolder.asStateFlow()

    private val _rawChapters = MutableStateFlow<List<Chapter>>(emptyList())
    private val _sortOption = MutableStateFlow("name_asc")
    val sortOption: StateFlow<String> = _sortOption.asStateFlow()

    val chapters: StateFlow<List<Chapter>> = combine(_rawChapters, _sortOption) { raw, sort ->
        sortChapters(raw, sort)
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(), emptyList())

    val allImages: StateFlow<List<ImageFile>> = chapters.map { chapterList ->
        chapterList.flatMap { it.images }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _viewMode = MutableStateFlow("explorer")
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    private val _layoutMode = MutableStateFlow("grid")
    val layoutMode: StateFlow<String> = _layoutMode.asStateFlow()

    private val _gridColumns = MutableStateFlow(3)
    val gridColumns: StateFlow<Int> = _gridColumns.asStateFlow()

    // Job to track the live favorites query
    private var favoritesJob: Job? = null

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
        return when (option) {
            "name_asc" -> chapters.sortedWith(naturalOrderChapterComparator())
            "name_desc" -> chapters.sortedWith(naturalOrderChapterComparator()).reversed()
            else -> chapters.sortedWith(naturalOrderChapterComparator())
        }
    }

    private fun naturalOrderChapterComparator(): Comparator<Chapter> {
        return Comparator { a, b -> FolderScanner.compareNatural(a.name, b.name) }
    }

    fun loadFolder(folderId: String, forceRescan: Boolean = false) {
        if (folderId == "favorites") {
            loadFavorites()
            return
        }

        // Cancel favorites observation if switching to a normal folder
        favoritesJob?.cancel()
        favoritesJob = null

        if (_currentFolder.value?.id == folderId && !forceRescan) {
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            // Clear previous data
            if (_currentFolder.value?.id != folderId) {
                _currentFolder.value = null
                _rawChapters.value = emptyList()
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

                    val allChapters = FolderScanner.cachedDataToChapters(getApplication(), cachedData)
                    val filteredChapters = allChapters.filter { chapter ->
                        !ignoredPaths.contains(chapter.path)
                    }

                    _rawChapters.value = filteredChapters
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadFavorites() {
        // If already observing favorites, don't restart
        if (favoritesJob?.isActive == true) return

        favoritesJob = viewModelScope.launch {
            _isLoading.value = true
            _currentFolder.value = ComicFolder("favorites", "Favorites", "", "", 0, 0, 0)

            // Live observation of favorites
            preferencesManager.favoritesFlow.collect { favoriteUris ->
                val images = withContext(Dispatchers.IO) {
                    favoriteUris.mapNotNull { uriString ->
                        try {
                            val uri = Uri.parse(uriString)
                            val file = File(uri.path ?: "")
                            ImageFile(
                                name = file.name,
                                path = file.absolutePath,
                                uri = uri,
                                size = file.length(),
                                dateModified = file.lastModified()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedByDescending { it.dateModified }
                }

                val favoritesChapter = Chapter(
                    name = "All Favorites",
                    path = "favorites_root",
                    imageCount = images.size,
                    images = images
                )

                _rawChapters.value = listOf(favoritesChapter)
                _isLoading.value = false
            }
        }
    }

    fun removeFolder(path: String) {
        viewModelScope.launch {
            val updatedChapters = _rawChapters.value.filter { it.path != path }
            _rawChapters.value = updatedChapters
            preferencesManager.addIgnoredPath(path)
        }
    }

    fun refreshFolder(folderId: String) {
        if (folderId == "favorites") {
            // Re-trigger flow collection if needed, but usually flow handles it
            favoritesJob?.cancel()
            loadFavorites()
        } else {
            folderCache.clearCache(folderId)
            loadFolder(folderId, forceRescan = true)
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