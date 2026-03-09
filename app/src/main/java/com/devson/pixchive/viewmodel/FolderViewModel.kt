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
import androidx.sqlite.db.SimpleSQLiteQuery
import com.devson.pixchive.data.local.HistoryEntity
import com.devson.pixchive.data.local.ImageEntity
import com.devson.pixchive.workers.FolderSyncWorker

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class FolderViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val imageDao = getApplication<PixChiveApplication>().database.imageDao()
    private val historyDao = getApplication<PixChiveApplication>().database.historyDao()
    private val favoriteDao = getApplication<PixChiveApplication>().database.favoriteDao()

    private val _currentFolder = MutableStateFlow<ComicFolder?>(null)
    val currentFolder: StateFlow<ComicFolder?> = _currentFolder.asStateFlow()

    private val _sortOption = MutableStateFlow("name_asc")
    val sortOption: StateFlow<String> = _sortOption.asStateFlow()

    private val _favoritesSortOption = MutableStateFlow("date_newest")
    val favoritesSortOption: StateFlow<String> = _favoritesSortOption.asStateFlow()

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

    // --- PAGING 3 FOR FLAT VIEW (sort-aware) ---
    // Uses combine so any change to folder OR sortOption triggers flatMapLatest,
    // which creates a brand-new Pager with the correct typed DAO query.
    // Typed queries are preferred over RawQuery here - Room can validate the SQL
    // at compile time and avoids building a SupportSQLiteQuery on every page load.
    val flatImages: Flow<PagingData<ImageEntity>> = combine(
        _currentFolder,
        _sortOption
    ) { folder, sort -> folder to sort }
        .flatMapLatest { (folder, sort) ->
            if (folder == null) return@flatMapLatest kotlinx.coroutines.flow.flowOf(androidx.paging.PagingData.empty())
            Pager(
                config = PagingConfig(
                    pageSize = 40,
                    enablePlaceholders = true,
                    // Keep 500 items in memory - large enough that pages near the
                    // scrolled-past region are not evicted during fast flings,
                    // preventing the DB re-fetch stutter / crash on scroll-back.
                    maxSize = 500,
                    // Pre-load 20 items ahead of the visible window so data is
                    // ready well before the user reaches it.
                    prefetchDistance = 20,
                    initialLoadSize = 40
                ),
                pagingSourceFactory = {
                    when (sort) {
                        "name_desc"    -> imageDao.getImagesPagedNameDesc(folder.id)
                        "date_newest"  -> imageDao.getImagesPagedDateNewest(folder.id)
                        "date_oldest"  -> imageDao.getImagesPagedDateOldest(folder.id)
                        else           -> imageDao.getImagesPagedNameAsc(folder.id) // "name_asc" + default
                    }
                }
            ).flow
        }.cachedIn(viewModelScope)

    // --- PAGING 3 FOR FAVORITES ---
    val favoriteImages: Flow<PagingData<ImageEntity>> = _favoritesSortOption
        .flatMapLatest { sort ->
            Pager(
                config = PagingConfig(
                    pageSize = 60,
                    enablePlaceholders = true,
                    maxSize = 200
                ),
                pagingSourceFactory = {
                    when (sort) {
                        "name_asc"    -> imageDao.getFavoritesPagedNameAsc()
                        "name_desc"   -> imageDao.getFavoritesPagedNameDesc()
                        "date_oldest" -> imageDao.getFavoritesPagedDateOldest()
                        else          -> imageDao.getFavoritesPagedDateNewest() // "date_newest" + default
                    }
                }
            ).flow
        }.cachedIn(viewModelScope)

    // URI set flow for ReaderScreen
    val favoriteUrisFlow: StateFlow<Set<String>> = favoriteDao.getAllFavoriteUrisFlow()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteImageCount: StateFlow<Int> = favoriteDao.getAllFavoriteUrisFlow()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Total count of images in the flat view - used by ReaderScreen for unbounded paging
    val flatImageCount: StateFlow<Int> = _currentFolder
        .flatMapLatest { folder ->
            if (folder == null) kotlinx.coroutines.flow.flowOf(0) else imageDao.getImageCountFlow(folder.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * Loads a single image by its sorted position - uses the SAME ORDER BY as the flat view
     * Pager so clicking image at grid index N always opens image N in the reader.
     */
    suspend fun getFlatImageAt(index: Int, folderId: String? = null): ImageEntity? {
        if (folderId == "favorites") {
            val orderBy = favoriteOrderBy(_favoritesSortOption.value)
            val sql = "SELECT images.* FROM images INNER JOIN favorite_images ON images.uri = favorite_images.uri ORDER BY $orderBy LIMIT 1 OFFSET ?"
            return imageDao.getImageByIndexRaw(SimpleSQLiteQuery(sql, arrayOf(index)))
        }

        val targetFolderId = folderId.takeIf { it != null && it != "favorites" } ?: _currentFolder.value?.id ?: return null
        val orderBy = flatOrderBy(_sortOption.value)
        val sql = "SELECT * FROM images WHERE folderId = ? ORDER BY $orderBy LIMIT 1 OFFSET ?"
        return imageDao.getImageByIndexRaw(SimpleSQLiteQuery(sql, arrayOf(targetFolderId, index)))
    }

    // --- SCROLL POSITION (survives back-navigation via shared ViewModel) ---
    private val _flatScrollIndex  = MutableStateFlow(0)
    private val _flatScrollOffset = MutableStateFlow(0)
    val flatScrollIndex:  StateFlow<Int> = _flatScrollIndex.asStateFlow()
    val flatScrollOffset: StateFlow<Int> = _flatScrollOffset.asStateFlow()

    private val _explorerScrollIndex  = MutableStateFlow(0)
    private val _explorerScrollOffset = MutableStateFlow(0)
    val explorerScrollIndex:  StateFlow<Int> = _explorerScrollIndex.asStateFlow()
    val explorerScrollOffset: StateFlow<Int> = _explorerScrollOffset.asStateFlow()

    fun saveFlatScrollPosition(index: Int, offset: Int) {
        _flatScrollIndex.value  = index
        _flatScrollOffset.value = offset
    }

    fun saveExplorerScrollPosition(index: Int, offset: Int) {
        _explorerScrollIndex.value  = index
        _explorerScrollOffset.value = offset
    }

    private fun favoriteOrderBy(sort: String) = when (sort) {
        "name_desc"   -> "images.parentFolderPath DESC, images.name DESC"
        "date_newest" -> "favorite_images.addedAt DESC"
        "date_oldest" -> "favorite_images.addedAt ASC"
        else          -> "images.parentFolderPath ASC, images.name ASC"
    }

    // Used by getFlatImageAt() to match the Pager's ORDER BY for by-offset lookup.
    private fun flatOrderBy(sort: String) = when (sort) {
        "name_desc"   -> "parentFolderPath DESC, name DESC"
        "date_newest" -> "dateModified DESC"
        "date_oldest" -> "dateModified ASC"
        else          -> "parentFolderPath ASC, name ASC" // "name_asc" + default
    }

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _viewMode = MutableStateFlow("explorer")
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    private val _layoutMode = MutableStateFlow("grid")
    val layoutMode: StateFlow<String> = _layoutMode.asStateFlow()

    private val _gridColumns = MutableStateFlow(3)
    val gridColumns: StateFlow<Int> = _gridColumns.asStateFlow()

    private val _readerScrollMode = MutableStateFlow("pager")
    val readerScrollMode: StateFlow<String> = _readerScrollMode.asStateFlow()

    private val _mangaMode = MutableStateFlow(false)
    val mangaMode: StateFlow<Boolean> = _mangaMode.asStateFlow()

    // Progress map for the current folder: chapterPath -> last read page
    val readProgressMap: StateFlow<Map<String, Int>> = _currentFolder
        .flatMapLatest { folder ->
            if (folder == null) flowOf(emptyMap())
            else preferencesManager.readProgressFlow(folder.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private var scanJob: Job? = null

    init {
        loadPreferences()
        migrateLegacyFavorites()
    }

    private fun migrateLegacyFavorites() {
        viewModelScope.launch {
            val legacyFavorites = preferencesManager.favoritesFlow.first()
            if (legacyFavorites.isNotEmpty()) {
                val newEntities = legacyFavorites.map { uri ->
                    com.devson.pixchive.data.local.FavoriteEntity(uri = uri)
                }
                favoriteDao.insertAllIgnoringConflicts(newEntities)
                preferencesManager.clearLegacyFavorites()
            }
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            _viewMode.value = preferencesManager.viewModeFlow.first()
            _layoutMode.value = preferencesManager.layoutModeFlow.first()
            _gridColumns.value = preferencesManager.folderGridColumnsFlow.first()
            _sortOption.value = preferencesManager.folderSortOptionFlow.first()
            _favoritesSortOption.value = preferencesManager.favoritesSortOptionFlow.first()
            _readerScrollMode.value = preferencesManager.readerScrollModeFlow.first()
            _mangaMode.value = preferencesManager.mangaModeFlow.first()
        }
    }

    private fun sortChapters(chapters: List<Chapter>, option: String): List<Chapter> {
        val nameComparator = Comparator<Chapter> { a, b ->
            FolderScanner.compareNatural(a.name, b.name)
        }
        return when (option) {
            "name_desc"   -> chapters.sortedWith(nameComparator).reversed()
            "date_newest" -> chapters.sortedByDescending { it.images.maxOfOrNull { img -> img.dateModified } ?: 0L }
            "date_oldest" -> chapters.sortedBy { it.images.minOfOrNull { img -> img.dateModified } ?: 0L }
            else          -> chapters.sortedWith(nameComparator) // "name_asc" + default
        }
    }

    fun loadFolder(folderId: String, forceRescan: Boolean = false) {
        if (folderId == "favorites") return
        
        // INSTANTLY clear the cached flows if we are opening a different folder
        if (_currentFolder.value?.id != folderId) {
            _currentFolder.value = null
        }

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
            // Reset scroll position BEFORE sort changes so the Pager doesn't try to
            // initialise at a stale high index in the newly reordered list - that burst
            // causes a major batch of concurrent DB page loads which leads to ANR.
            _flatScrollIndex.value  = 0
            _flatScrollOffset.value = 0
            _sortOption.value = option
            preferencesManager.saveFolderSortOption(option)
        }
    }

    fun toggleFavorite(uri: String) {
        viewModelScope.launch {
            val isFav = favoriteDao.getFavorite(uri) != null
            if (isFav) {
                favoriteDao.delete(uri)
            } else {
                favoriteDao.insert(com.devson.pixchive.data.local.FavoriteEntity(uri))
            }
        }
    }

    fun setFavoritesSortOption(option: String) {
        viewModelScope.launch {
            _favoritesSortOption.value = option
            preferencesManager.saveFavoritesSortOption(option)
        }
    }

    fun setReaderScrollMode(mode: String) {
        viewModelScope.launch {
            _readerScrollMode.value = mode
            preferencesManager.saveReaderScrollMode(mode)
        }
    }

    fun setMangaMode(enabled: Boolean) {
        viewModelScope.launch {
            _mangaMode.value = enabled
            preferencesManager.saveMangaMode(enabled)
        }
    }

    fun saveReadProgress(folderId: String, chapterPath: String, page: Int) {
        viewModelScope.launch {
            val finalChapterPath = if (folderId == "favorites") "favorites_view" else chapterPath
            preferencesManager.saveReadProgress(folderId, finalChapterPath, page)

            val chapterImages = chapters.value
                .firstOrNull { it.path == chapterPath }
                ?.images ?: emptyList()

            val totalPages = when {
                folderId == "favorites" -> favoriteImageCount.value
                chapterPath == "flat_view" -> flatImageCount.value
                else -> chapterImages.size
            }

            val coverUri = when {
                chapterImages.isNotEmpty() -> chapterImages.getOrNull(page)?.uri ?: chapterImages.firstOrNull()?.uri ?: ""
                folderId == "favorites" -> getFlatImageAt(page, "favorites")?.uri ?: ""
                chapterPath == "flat_view" -> getFlatImageAt(page, folderId)?.uri ?: ""
                else -> ""
            }

            val historyTitle = when {
                folderId == "favorites" -> "Favorites"
                chapterPath == "flat_view" -> "${_currentFolder.value?.name ?: "Unknown Folder"} (All)"
                else -> chapterPath.substringAfterLast("/").substringAfterLast(":")
            }

            historyDao.upsert(
                HistoryEntity(
                    chapterPath = finalChapterPath,
                    folderId = folderId,
                    title = historyTitle,
                    coverImageUri = coverUri,
                    currentPage = page,
                    totalPages = totalPages.coerceAtLeast(1),
                    lastAccessed = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun getReadProgress(chapterPath: String): Int {
        val folderId = _currentFolder.value?.id ?: return 0
        return preferencesManager.getReadProgress(folderId, chapterPath)
    }
}