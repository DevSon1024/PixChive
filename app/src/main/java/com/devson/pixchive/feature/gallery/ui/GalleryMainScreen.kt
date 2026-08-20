package com.devson.pixchive.feature.gallery.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.core.data.FileOperationsViewModel
import com.devson.pixchive.core.data.PreferencesManager
import com.devson.pixchive.feature.gallery.ui.components.CustomRenameDialog
import com.devson.pixchive.feature.gallery.ui.components.DetailsDialog
import com.devson.pixchive.feature.gallery.ui.components.GallerySelectionBottomBar
import com.devson.pixchive.feature.gallery.ui.components.GalleryViewSettingsBottomSheet
import com.devson.pixchive.feature.gallery.ui.components.GlobalSearchAppBar
import com.devson.pixchive.feature.gallery.viewmodel.AllImagesViewModel
import com.devson.pixchive.feature.gallery.viewmodel.GalleryState
import com.devson.pixchive.feature.gallery.viewmodel.ImageListViewModel
import com.devson.pixchive.feature.gallery.viewmodel.SearchViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryMainScreen(
    onNavigateBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onRecycleBinClick: () -> Unit = {},
    onFolderClick: (String) -> Unit,
    onImageClick: (String, Int) -> Unit,
    onSearch: (String) -> Unit,
    allImagesViewModel: AllImagesViewModel = viewModel(),
    imageListViewModel: ImageListViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val lastTab by prefsManager.lastGalleryTabFlow.collectAsState(initial = null)
    var isInitialized by remember { mutableStateOf(false) }

    val tabs = listOf("Albums", "Photos")
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val scope = rememberCoroutineScope()

    // Sync pager state from preferences once
    LaunchedEffect(lastTab) {
        if (lastTab != null && !isInitialized) {
            if (lastTab != pagerState.currentPage) {
                pagerState.scrollToPage(lastTab!!)
            }
            isInitialized = true
        }
    }

    // Sync pager state with preferences only after initialization
    LaunchedEffect(pagerState.currentPage) {
        if (isInitialized) {
            prefsManager.setLastGalleryTab(pagerState.currentPage)
        }
    }

    val fileOpsViewModel: FileOperationsViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()

    val allImagesSelection by allImagesViewModel.selectedIds.collectAsState()
    val imageListSelection by imageListViewModel.selectedIds.collectAsState()
    val selectedImages by allImagesViewModel.selectedImages.collectAsState()

    val isInSelectionMode = (pagerState.currentPage == 0 && imageListSelection.isNotEmpty()) ||
            (pagerState.currentPage == 1 && allImagesSelection.isNotEmpty())

    var showAlbumsSettingsSheet by remember { mutableStateOf(false) }
    var showPhotosSettingsSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showStorageExplorer by remember { mutableStateOf(false) }
    var explorerOperationType by remember { mutableStateOf("MOVE") }

    val searchQuery by searchViewModel.searchQuery.collectAsState()
    val suggestions by searchViewModel.suggestions.collectAsState()

    Scaffold(
        topBar = {
            if (isInSelectionMode) {
                val selectedCount = if (pagerState.currentPage == 0) imageListSelection.size else allImagesSelection.size
                TopAppBar(
                    title = { Text("$selectedCount selected", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (pagerState.currentPage == 0) {
                                imageListViewModel.clearSelection()
                            } else {
                                allImagesViewModel.clearSelection()
                            }
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (pagerState.currentPage == 0) {
                                imageListViewModel.selectAll()
                            } else {
                                allImagesViewModel.selectAll()
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            } else {
                GlobalSearchAppBar(
                    title = "Gallery",
                    searchQuery = searchQuery,
                    suggestions = suggestions,
                    onQueryChange = { searchViewModel.updateSearchQuery(it) },
                    onSearch = onSearch,
                    onBackClick = onNavigateBack,
                    actions = {
                        IconButton(onClick = {
                            if (pagerState.currentPage == 0) {
                                showAlbumsSettingsSheet = true
                            } else {
                                showPhotosSettingsSheet = true
                            }
                        }) {
                            Icon(Icons.Default.Tune, contentDescription = "View Settings")
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "App Settings")
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !isInSelectionMode
            ) { page ->
                when (page) {
                    0 -> AlbumsScreen(
                        onNavigateBack = onNavigateBack,
                        onFolderClick = onFolderClick,
                        onSettingsClick = onSettingsClick,
                        onSwitchToPhotos = {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onRecycleBinClick = onRecycleBinClick,
                        onSearch = onSearch,
                        showTopBar = false,
                        viewModel = imageListViewModel
                    )
                    1 -> PhotosScreen(
                        onNavigateBack = onNavigateBack,
                        onSettingsClick = onSettingsClick,
                        onSearch = onSearch,
                        onImageClick = { index -> onImageClick("all_images", index) },
                        onSwitchToAlbums = {
                            scope.launch { pagerState.animateScrollToPage(0) }
                        },
                        onRecycleBinClick = onRecycleBinClick,
                        showTopBar = false,
                        viewModel = allImagesViewModel
                    )
                }
            }

            // Floating Selection Bottom Bar
            AnimatedVisibility(
                visible = isInSelectionMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                val isAlbums = pagerState.currentPage == 0
                val selectedCount = if (isAlbums) imageListSelection.size else allImagesSelection.size

                GallerySelectionBottomBar(
                    selectedImages = if (isAlbums) emptyList() else selectedImages,
                    selectedCount = selectedCount,
                    fileOpsViewModel = fileOpsViewModel,
                    onMove = {
                        explorerOperationType = "MOVE"
                        showStorageExplorer = true
                    },
                    onCopy = {
                        explorerOperationType = "COPY"
                        showStorageExplorer = true
                    },
                    onRename = {
                        showRenameDialog = true
                    },
                    onInfo = {
                        showDetailsDialog = true
                    },
                    onDelete = {
                        if (isAlbums) {
                            val foldersState = imageListViewModel.uiState.value
                            val selectedFoldersList = (foldersState as? GalleryState.Success)?.folders?.filter { it.bucketId in imageListSelection } ?: emptyList()
                            val uris = selectedFoldersList.map { it.thumbnailUri }
                            if (uris.isNotEmpty()) {
                                fileOpsViewModel.deleteImages(context, uris, trash = true)
                            }
                            imageListViewModel.clearSelection()
                        } else {
                            val uris = selectedImages.map { it.uri }
                            if (uris.isNotEmpty()) {
                                fileOpsViewModel.deleteImages(context, uris, trash = true)
                            }
                            allImagesViewModel.clearSelection()
                        }
                    }
                )
            }
        }
    }

    // View Settings Bottom Sheets
    if (showAlbumsSettingsSheet) {
        val viewSettings by imageListViewModel.viewSettings.collectAsState()
        val sortOption by imageListViewModel.sortOption.collectAsState()
        val showFolderThumbnail by imageListViewModel.showFolderThumbnail.collectAsState()
        val layoutMode by imageListViewModel.layoutMode.collectAsState()
        val gridCellsIndex by imageListViewModel.gridCellsIndex.collectAsState()

        GalleryViewSettingsBottomSheet(
            layoutMode = layoutMode,
            onLayoutModeChange = { imageListViewModel.setLayoutMode(it) },
            gridCellsIndex = gridCellsIndex,
            onGridCellsIndexChange = { imageListViewModel.setGridCellsIndex(it) },
            viewSettings = viewSettings,
            onViewSettingsChange = { imageListViewModel.updateViewSettings(it) },
            sortOption = sortOption,
            onSortOptionChange = { imageListViewModel.setSortOption(it) },
            showFolderThumbnail = showFolderThumbnail,
            onShowFolderThumbnailChange = { imageListViewModel.setShowFolderThumbnail(it) },
            galleryViewMode = "albums",
            onGalleryViewModeChange = { mode ->
                showAlbumsSettingsSheet = false
                scope.launch {
                    if (mode == "photos") pagerState.animateScrollToPage(1)
                }
            },
            onDismiss = { showAlbumsSettingsSheet = false }
        )
    }

    if (showPhotosSettingsSheet) {
        val viewSettings by allImagesViewModel.viewSettings.collectAsState()
        val sortOption by allImagesViewModel.sortOption.collectAsState()
        val layoutMode by allImagesViewModel.layoutMode.collectAsState()
        val gridCellsIndex by allImagesViewModel.gridCellsIndex.collectAsState()

        GalleryViewSettingsBottomSheet(
            layoutMode = layoutMode,
            onLayoutModeChange = { allImagesViewModel.setLayoutMode(it) },
            gridCellsIndex = gridCellsIndex,
            onGridCellsIndexChange = { allImagesViewModel.setGridCellsIndex(it) },
            viewSettings = viewSettings,
            onViewSettingsChange = { allImagesViewModel.updateViewSettings(it) },
            sortOption = sortOption,
            onSortOptionChange = { allImagesViewModel.setSortOption(it) },
            showFolderThumbnail = false,
            onShowFolderThumbnailChange = {},
            galleryViewMode = "photos",
            onGalleryViewModeChange = { mode ->
                showPhotosSettingsSheet = false
                scope.launch {
                    if (mode == "albums") pagerState.animateScrollToPage(0)
                }
            },
            onDismiss = { showPhotosSettingsSheet = false }
        )
    }

    // Selection Dialogs
    if (showDetailsDialog) {
        if (pagerState.currentPage == 0) {
            val foldersState by imageListViewModel.uiState.collectAsState()
            val selectedFoldersList = (foldersState as? GalleryState.Success)?.folders?.filter { it.bucketId in imageListSelection } ?: emptyList()
            DetailsDialog(
                selectedFolders = selectedFoldersList,
                selectedImages = emptyList(),
                onDismiss = { showDetailsDialog = false }
            )
        } else {
            DetailsDialog(
                selectedFolders = emptyList(),
                selectedImages = selectedImages,
                onDismiss = { showDetailsDialog = false }
            )
        }
    }

    if (showRenameDialog) {
        if (pagerState.currentPage == 0) {
            val selectedId = imageListSelection.firstOrNull()
            val foldersState by imageListViewModel.uiState.collectAsState()
            val folder = (foldersState as? GalleryState.Success)?.folders?.find { it.bucketId == selectedId }
            folder?.let {
                CustomRenameDialog(
                    initialName = it.folderName,
                    onConfirm = { newName ->
                        imageListViewModel.renameSelectedFolder(newName)
                        showRenameDialog = false
                    },
                    onDismiss = { showRenameDialog = false }
                )
            }
        } else {
            val selectedId = allImagesSelection.firstOrNull()
            val image = selectedImages.find { it.id == selectedId }
            image?.let {
                CustomRenameDialog(
                    initialName = it.realPath.substringAfterLast('/'),
                    onConfirm = { newName ->
                        allImagesViewModel.renameSelectedImage(newName)
                        showRenameDialog = false
                    },
                    onDismiss = { showRenameDialog = false }
                )
            }
        }
    }

    if (showStorageExplorer) {
        StorageExplorerScreen(
            operationType = explorerOperationType,
            sourceUris = selectedImages.map { it.uri },
            onComplete = {
                showStorageExplorer = false
                allImagesViewModel.clearSelection()
            },
            onCancel = { showStorageExplorer = false }
        )
    }
}