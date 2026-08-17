package com.devson.pixchive.feature.gallery.ui

import androidx.compose.foundation.background
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
import com.devson.pixchive.core.data.PreferencesManager
import com.devson.pixchive.feature.gallery.ui.components.GalleryViewSettingsBottomSheet
import com.devson.pixchive.feature.gallery.ui.components.GlobalSearchAppBar
import com.devson.pixchive.feature.gallery.viewmodel.AllImagesViewModel
import com.devson.pixchive.feature.gallery.viewmodel.ImageListViewModel
import com.devson.pixchive.feature.gallery.viewmodel.SearchViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryMainScreen(
    onNavigateBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onRecycleBinClick: () -> Unit,
    onFolderClick: (String) -> Unit,
    onImageClick: (String, Int) -> Unit,
    onSearch: (String) -> Unit
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

    val allImagesViewModel: AllImagesViewModel = viewModel()
    val imageListViewModel: ImageListViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()

    val allImagesSelection by allImagesViewModel.selectedIds.collectAsState()
    val imageListSelection by imageListViewModel.selectedIds.collectAsState()

    val isInSelectionMode = (pagerState.currentPage == 0 && imageListSelection.isNotEmpty()) ||
            (pagerState.currentPage == 1 && allImagesSelection.isNotEmpty())

    var showAlbumsSettingsSheet by remember { mutableStateOf(false) }
    var showPhotosSettingsSheet by remember { mutableStateOf(false) }

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
                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                    GlobalSearchAppBar(
                        title = "Gallery",
                        searchQuery = searchQuery,
                        suggestions = suggestions,
                        onQueryChange = { searchViewModel.updateSearchQuery(it) },
                        onSearch = onSearch,
                        onBackClick = onNavigateBack,
                        actions = {
                            IconButton(onClick = onRecycleBinClick) {
                                Icon(Icons.Filled.RestoreFromTrash, contentDescription = "Recycle Bin")
                            }
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

                    // Material 3 Segmented Button Row at Top
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        SegmentedButton(
                            selected = pagerState.currentPage == 0,
                            onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {
                                SegmentedButtonDefaults.Icon(active = pagerState.currentPage == 0) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        ) {
                            Text(
                                "Albums",
                                fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        SegmentedButton(
                            selected = pagerState.currentPage == 1,
                            onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {
                                SegmentedButtonDefaults.Icon(active = pagerState.currentPage == 1) {
                                    Icon(
                                        Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        ) {
                            Text(
                                "Photos",
                                fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
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
            onDismiss = { showAlbumsSettingsSheet = false }
        )
    }

    if (showPhotosSettingsSheet) {
        val viewSettings by allImagesViewModel.viewSettings.collectAsState()
        val layoutMode by allImagesViewModel.layoutMode.collectAsState()
        val gridCellsIndex by allImagesViewModel.gridCellsIndex.collectAsState()

        GalleryViewSettingsBottomSheet(
            layoutMode = layoutMode,
            onLayoutModeChange = { allImagesViewModel.setLayoutMode(it) },
            gridCellsIndex = gridCellsIndex,
            onGridCellsIndexChange = { allImagesViewModel.setGridCellsIndex(it) },
            viewSettings = viewSettings,
            onViewSettingsChange = { allImagesViewModel.updateViewSettings(it) },
            sortOption = "DATE_DESC",
            onSortOptionChange = {},
            showFolderThumbnail = false,
            onShowFolderThumbnailChange = {},
            onDismiss = { showPhotosSettingsSheet = false }
        )
    }
}