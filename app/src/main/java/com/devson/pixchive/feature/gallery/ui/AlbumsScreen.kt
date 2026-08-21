package com.devson.pixchive.feature.gallery.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.core.data.FileOperationsViewModel
import com.devson.pixchive.core.designsystem.component.PixChiveEmptyState
import com.devson.pixchive.core.designsystem.component.SkeletonLoadingView
import com.devson.pixchive.core.utils.PermissionHelper
import com.devson.pixchive.feature.gallery.ui.components.CustomRenameDialog
import com.devson.pixchive.feature.gallery.ui.components.DetailsDialog
import com.devson.pixchive.feature.gallery.ui.components.GalleryFolderItem
import com.devson.pixchive.feature.gallery.ui.components.GalleryFolderListItem
import com.devson.pixchive.feature.gallery.ui.components.GallerySelectionBottomBar
import com.devson.pixchive.feature.gallery.ui.components.GalleryViewSettingsBottomSheet
import com.devson.pixchive.feature.gallery.ui.components.GlobalSearchAppBar
import com.devson.pixchive.feature.gallery.viewmodel.GalleryState
import com.devson.pixchive.feature.gallery.viewmodel.ImageListViewModel
import com.devson.pixchive.feature.gallery.viewmodel.SearchViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    onNavigateBack: () -> Unit,
    onFolderClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onSearch: (String) -> Unit = {},
    onRecycleBinClick: () -> Unit = {},
    onSwitchToPhotos: () -> Unit = {},
    showTopBar: Boolean = true,
    viewModel: ImageListViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val fileOpsViewModel: FileOperationsViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()
    val searchQuery by searchViewModel.searchQuery.collectAsState()
    val suggestions by searchViewModel.suggestions.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val savedGridCellsIndex by viewModel.gridCellsIndex.collectAsState()
    val layoutMode by viewModel.layoutMode.collectAsState()
    val isGalleryListMode by viewModel.isGalleryListMode.collectAsState()
    val galleryGridColumns by viewModel.galleryGridColumns.collectAsState()
    val galleryCoverAspectRatio by viewModel.galleryCoverAspectRatio.collectAsState()
    val viewSettings by viewModel.viewSettings.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val showFolderThumbnail by viewModel.showFolderThumbnail.collectAsState()
    val galleryViewMode by viewModel.galleryViewMode.collectAsState()

    var hasPermission by remember { mutableStateOf(PermissionHelper.hasStoragePermission(context)) }

    val selectedFolderIds by viewModel.selectedIds.collectAsState()
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    // Transient Grid Size notification pill
    val coroutineScope = rememberCoroutineScope()
    var showGridPill by remember { mutableStateOf(false) }
    var currentGridPillText by remember { mutableStateOf("") }
    var pillDismissJob by remember { mutableStateOf<Job?>(null) }

    val triggerGridPill: (Int) -> Unit = { newCols ->
        currentGridPillText = "Grid Size: $newCols"
        showGridPill = true
        pillDismissJob?.cancel()
        pillDismissJob = coroutineScope.launch {
            delay(1400)
            showGridPill = false
        }
    }

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    val isListMode = isGalleryListMode || layoutMode == "list"

    BackHandler(enabled = selectedFolderIds.isNotEmpty()) {
        viewModel.clearSelection()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentlyHasPermission = PermissionHelper.hasStoragePermission(context)
                if (currentlyHasPermission && !hasPermission) {
                    hasPermission = true
                    viewModel.loadGalleryFolders()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        fileOpsViewModel.successfulDeletions.collect { uris ->
            viewModel.removeFoldersLocally(uris)
        }
    }

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (isGranted) {
                viewModel.loadGalleryFolders()
            }
        }
    )

    val allFilesAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            val isGranted = PermissionHelper.hasStoragePermission(context)
            hasPermission = isGranted
            if (isGranted) viewModel.loadGalleryFolders()
        }
    )

    val selectedFolders = remember(selectedFolderIds, uiState) {
        if (selectedFolderIds.isNotEmpty() && uiState is GalleryState.Success) {
            (uiState as GalleryState.Success).folders.filter { it.bucketId in selectedFolderIds }
        } else emptyList()
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                if (selectedFolderIds.isNotEmpty()) {
                    TopAppBar(
                        title = { Text("${selectedFolderIds.size} selected", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear selection")
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.selectAll() }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                } else {
                    GlobalSearchAppBar(
                        title = "Albums",
                        searchQuery = searchQuery,
                        suggestions = suggestions,
                        onQueryChange = { searchViewModel.updateSearchQuery(it) },
                        onSearch = onSearch,
                        onBackClick = onNavigateBack,
                        actions = {
                            IconButton(onClick = onRecycleBinClick) {
                                Icon(Icons.Filled.RestoreFromTrash, contentDescription = "Recycle Bin")
                            }
                            IconButton(onClick = { showSettingsSheet = true }) {
                                Icon(Icons.Default.Tune, contentDescription = "View Settings")
                            }
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Default.Settings, contentDescription = "App Settings")
                            }
                        }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (showTopBar) paddingValues else PaddingValues(0.dp))
        ) {
            if (!hasPermission) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Storage Permission Required", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "PixChive needs access to your storage to find device albums.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try {
                                allFilesAccessLauncher.launch(PermissionHelper.getStoragePermissionSettingsIntent(context))
                            } catch (_: Exception) {}
                        } else {
                            legacyPermissionLauncher.launch(PermissionHelper.getLegacyStoragePermission())
                        }
                    }) {
                        Text("Grant Permission")
                    }
                }
            } else {
                when (val state = uiState) {
                    is GalleryState.Loading -> {
                        val baseColumns = if (isListMode) 1 else (4 - savedGridCellsIndex.coerceIn(0, 2))
                        SkeletonLoadingView(
                            layoutMode = if (isListMode) "list" else "grid",
                            columns = baseColumns
                        )
                    }
                    is GalleryState.Error -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                    is GalleryState.Success -> {
                        if (state.folders.isEmpty()) {
                            PixChiveEmptyState(
                                icon = Icons.Default.FolderOpen,
                                title = "No Albums Found",
                                message = "Image albums stored on your device will appear here."
                            )
                        } else {
                            if (isListMode) {
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(
                                        top = 4.dp,
                                        bottom = 100.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    listItems(state.folders, key = { it.bucketId }) { folder ->
                                        val isSelected = folder.bucketId in selectedFolderIds
                                        GalleryFolderListItem(
                                            folder = folder,
                                            isSelected = isSelected,
                                            isSelectionModeActive = selectedFolderIds.isNotEmpty(),
                                            viewSettings = viewSettings,
                                            showThumbnail = showFolderThumbnail,
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = {
                                                if (selectedFolderIds.isNotEmpty()) {
                                                    viewModel.toggleSelection(folder.bucketId)
                                                } else {
                                                    onFolderClick(folder.bucketId)
                                                }
                                            },
                                            onThumbnailClick = { viewModel.toggleSelection(folder.bucketId) },
                                            onLongPress = { viewModel.toggleSelection(folder.bucketId) }
                                        )
                                    }
                                }
                            } else {
                                var currentColumns by remember(savedGridCellsIndex) {
                                    mutableIntStateOf(4 - savedGridCellsIndex.coerceIn(0, 2))
                                }
                                var accumulatedZoom by remember { mutableFloatStateOf(1f) }

                                val animatedColumns by animateIntAsState(
                                    targetValue = currentColumns,
                                    animationSpec = tween(300),
                                    label = "columns_anim"
                                )

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(animatedColumns.coerceIn(2, 4)),
                                    state = gridState,
                                    contentPadding = PaddingValues(
                                        top = 4.dp,
                                        bottom = 100.dp,
                                        start = 12.dp,
                                        end = 12.dp
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(Unit) {
                                            awaitEachGesture {
                                                awaitFirstDown(requireUnconsumed = false)
                                                var hasChangedInThisGesture = false
                                                do {
                                                    val event = awaitPointerEvent()
                                                    if (event.changes.size >= 2) {
                                                        val zoom = event.calculateZoom()
                                                        accumulatedZoom *= zoom
                                                        if (!hasChangedInThisGesture) {
                                                            if (accumulatedZoom > 1.25f) {
                                                                val newCols = (currentColumns - 1).coerceIn(2, 4)
                                                                if (newCols != currentColumns) {
                                                                    currentColumns = newCols
                                                                    viewModel.setGridCellsIndex(4 - newCols)
                                                                    viewModel.setGalleryGridColumns(newCols)
                                                                    triggerGridPill(newCols)
                                                                }
                                                                hasChangedInThisGesture = true
                                                            } else if (accumulatedZoom < 0.75f) {
                                                                val newCols = (currentColumns + 1).coerceIn(2, 4)
                                                                if (newCols != currentColumns) {
                                                                    currentColumns = newCols
                                                                    viewModel.setGridCellsIndex(4 - newCols)
                                                                    viewModel.setGalleryGridColumns(newCols)
                                                                    triggerGridPill(newCols)
                                                                }
                                                                hasChangedInThisGesture = true
                                                            }
                                                        }
                                                        event.changes.forEach { if (it.pressed) it.consume() }
                                                    } else {
                                                        accumulatedZoom = 1f
                                                        hasChangedInThisGesture = false
                                                    }
                                                } while (event.changes.any { it.pressed })
                                            }
                                        }
                                ) {
                                    items(state.folders, key = { it.bucketId }) { folder ->
                                        val isSelected = folder.bucketId in selectedFolderIds
                                        GalleryFolderItem(
                                            folder = folder,
                                            isSelected = isSelected,
                                            isSelectionModeActive = selectedFolderIds.isNotEmpty(),
                                            isListMode = false,
                                            viewSettings = viewSettings,
                                            showThumbnail = showFolderThumbnail,
                                            aspectRatio = galleryCoverAspectRatio,
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = {
                                                if (selectedFolderIds.isNotEmpty()) {
                                                    viewModel.toggleSelection(folder.bucketId)
                                                } else {
                                                    onFolderClick(folder.bucketId)
                                                }
                                            },
                                            onThumbnailClick = { viewModel.toggleSelection(folder.bucketId) },
                                            onLongPress = { viewModel.toggleSelection(folder.bucketId) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Transient Floating Grid Size Feedback Pill
                AnimatedVisibility(
                    visible = showGridPill,
                    enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.85f),
                    exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f),
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        shadowElevation = 6.dp,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.inverseOnSurface
                            )
                            Text(
                                text = currentGridPillText,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.inverseOnSurface
                            )
                        }
                    }
                }
            }
        }

        if (showSettingsSheet) {
            GalleryViewSettingsBottomSheet(
                layoutMode = if (isListMode) "list" else "grid",
                onLayoutModeChange = {
                    viewModel.setLayoutMode(it)
                    viewModel.setGalleryListMode(it == "list")
                },
                gridCellsIndex = savedGridCellsIndex,
                onGridCellsIndexChange = {
                    viewModel.setGridCellsIndex(it)
                    viewModel.setGalleryGridColumns(4 - it.coerceIn(0, 2))
                },
                viewSettings = viewSettings,
                onViewSettingsChange = { viewModel.updateViewSettings(it) },
                sortOption = sortOption,
                onSortOptionChange = { viewModel.setSortOption(it) },
                isRootFolderView = true,
                showFolderThumbnail = showFolderThumbnail,
                onShowFolderThumbnailChange = { viewModel.setShowFolderThumbnail(it) },
                galleryViewMode = galleryViewMode,
                onGalleryViewModeChange = { mode ->
                    viewModel.setGalleryViewMode(mode)
                    if (mode == "photos") {
                        showSettingsSheet = false
                        onSwitchToPhotos()
                    }
                },
                aspectRatio = galleryCoverAspectRatio,
                onAspectRatioChange = { viewModel.setGalleryCoverAspectRatio(it) },
                onDismiss = { showSettingsSheet = false }
            )
        }

        if (showDetailsDialog) {
            DetailsDialog(
                selectedFolders = selectedFolders,
                selectedImages = emptyList(),
                onDismiss = { showDetailsDialog = false }
            )
        }

        if (showRenameDialog) {
            val selectedFolder = selectedFolders.firstOrNull()
            selectedFolder?.let {
                CustomRenameDialog(
                    initialName = it.folderName,
                    onConfirm = { newName ->
                        viewModel.renameSelectedFolder(newName)
                        showRenameDialog = false
                    },
                    onDismiss = { showRenameDialog = false }
                )
            }
        }
    }
}