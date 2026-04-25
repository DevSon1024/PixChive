package com.devson.pixchive.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.pixchive.data.local.AppDatabase
import com.devson.pixchive.data.local.ImageDao
import com.devson.pixchive.data.local.ImageEntity
import com.devson.pixchive.model.ViewSettings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ImageListViewModel(application: Application) : AndroidViewModel(application) {

    private val imageDao: ImageDao = AppDatabase.getDatabase(application).imageDao()

    private val _viewSettings = MutableStateFlow(ViewSettings())
    val viewSettings: StateFlow<ViewSettings> = _viewSettings.asStateFlow()

    private val _allImagesCache = MutableStateFlow<List<ImageEntity>>(emptyList())

    val imagesByFolder: StateFlow<Map<String, List<ImageEntity>>> = combine(
        _allImagesCache,
        _viewSettings
    ) { allImages, settings ->
        val filtered = if (settings.showHiddenFiles) {
            allImages
        } else {
            allImages.filter { !it.path.split('/').any { segment -> segment.startsWith(".") && segment.isNotEmpty() } }
        }
        filtered.groupBy { it.parentFolderName }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    private val _currentExplorerPath = MutableStateFlow<String?>(null)
    val currentExplorerPath: StateFlow<String?> = _currentExplorerPath.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchSuggestions = MutableStateFlow<List<ImageEntity>>(emptyList())
    val searchSuggestions: StateFlow<List<ImageEntity>> = _searchSuggestions.asStateFlow()

    init {
        viewModelScope.launch {
            imageDao.getAllImagesFlow().collect { images ->
                _allImagesCache.value = images
                _isLoading.value = false
            }
        }
    }

    fun loadImages(forceRefresh: Boolean = false) {
        if (forceRefresh) {
            viewModelScope.launch {
                _isRefreshing.value = true
                _allImagesCache.value = imageDao.getAllImagesFlow().first()
                _isRefreshing.value = false
            }
        }
    }

    fun selectFolder(folderName: String?) {
        _selectedFolder.value = folderName
    }

    fun navigateToExplorerPath(path: String) {
        _currentExplorerPath.value = path
    }

    fun navigateExplorerUp(): Boolean {
        val current = _currentExplorerPath.value ?: return false
        val currentTrimmed = current.dropLast(1)
        if (!currentTrimmed.contains('/')) {
            _currentExplorerPath.value = null
            return false
        }
        _currentExplorerPath.value = currentTrimmed.substringBeforeLast('/') + "/"
        return true
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        val q = query.trim().lowercase()
        _searchSuggestions.value = if (q.isEmpty()) emptyList()
        else _allImagesCache.value.filter { it.name.lowercase().contains(q) }.take(8)
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchSuggestions.value = emptyList()
    }

    fun updateViewSettings(settings: ViewSettings) {
        _viewSettings.value = settings
    }
}