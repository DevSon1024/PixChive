package com.devson.pixchive.gallery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.gallery.data.models.GalleryImage
import com.devson.pixchive.gallery.ui.components.GalleryViewSettingsBottomSheet
import com.devson.pixchive.gallery.ui.components.DetailsDialog
import com.devson.pixchive.gallery.ui.components.GalleryImageItem
import com.devson.pixchive.gallery.ui.components.GallerySelectionBottomBar
import com.devson.pixchive.gallery.ui.components.CustomRenameDialog
import com.devson.pixchive.gallery.viewmodel.AllImagesState
import com.devson.pixchive.gallery.viewmodel.AllImagesViewModel
import com.devson.pixchive.viewmodel.FileOperationsViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllImagesScreen(
    onNavigateBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearch: (String) -> Unit = {},
    onImageClick: (Int) -> Unit = {},
    onAlbumsClick: () -> Unit = {},
    onRecycleBinClick: () -> Unit = {},
    viewModel: AllImagesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val layoutMode by viewModel.layoutMode.collectAsState()
    val gridCellsIndex by viewModel.gridCellsIndex.collectAsState()
    val viewSettings by viewModel.viewSettings.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showStorageExplorer by remember { mutableStateOf(false) }
    var explorerOperationType by remember { mutableStateOf("") }

    val fileOpsViewModel: FileOperationsViewModel = viewModel()
    val context = LocalContext.current
    val pendingIntentSender by fileOpsViewModel.pendingIntentSender.collectAsState()

    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            fileOpsViewModel.onPermissionGranted(context)
            viewModel.clearSelection()
        }
    }

    LaunchedEffect(pendingIntentSender) {
        pendingIntentSender?.let { intentSender ->
            val request = IntentSenderRequest.Builder(intentSender).build()
            intentSenderLauncher.launch(request)
            fileOpsViewModel.clearPendingIntentSender()
        }
    }

    LaunchedEffect(Unit) {
        fileOpsViewModel.successfulDeletions.collect { uris ->
            viewModel.removeImagesLocally(uris)
        }
    }

    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = 0
    )

    BackHandler(enabled = selectedIds.isNotEmpty()) {
        viewModel.clearSelection()
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            if (selectedIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text("${selectedIds.size} selected") },
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
                val searchViewModel: com.devson.pixchive.gallery.viewmodel.SearchViewModel = viewModel()
                val searchQuery by searchViewModel.searchQuery.collectAsState()
                val suggestions by searchViewModel.suggestions.collectAsState()

                com.devson.pixchive.gallery.ui.components.GlobalSearchAppBar(
                    title = "Photos",
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
        },
        bottomBar = {
            if (selectedIds.isNotEmpty()) {
                val selectedImages = (uiState as? AllImagesState.Success)?.flatImages?.filter { it.id in selectedIds } ?: emptyList()
                GallerySelectionBottomBar(
                    selectedImages = selectedImages,
                    fileOpsViewModel = fileOpsViewModel,
                    onMove = {
                        explorerOperationType = "MOVE"
                        showStorageExplorer = true
                    },
                    onCopy = {
                        explorerOperationType = "COPY"
                        showStorageExplorer = true
                    },
                    onDelete = {},
                    onRename = { showRenameDialog = true },
                    onInfo = { showDetailsDialog = true }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is AllImagesState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(paddingValues)
                    )
                }
                is AllImagesState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(paddingValues)
                            .padding(16.dp)
                    )
                }
                is AllImagesState.Success -> {
                    if (state.grouped.isEmpty()) {
                        Text(
                            text = "No images found on this device.",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        val isListMode = layoutMode == "list"

                        if (isListMode) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(1),
                                state = gridState,
                                contentPadding = PaddingValues(
                                    top = paddingValues.calculateTopPadding(),
                                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                                ),
                                horizontalArrangement = Arrangement.Start,
                                verticalArrangement = Arrangement.Top,
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                state.grouped.forEach { (dateLabel, images) ->
                                    item(
                                        key = "header_$dateLabel",
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) {
                                        DateGroupHeader(label = dateLabel)
                                    }

                                     items(
                                        items = images,
                                        key = { it.id },
                                        contentType = { "gallery_image" }
                                    ) { image ->
                                        // Read selectedIds here so only this item recomposes on selection change
                                        val isSelected = image.id in selectedIds
                                        GalleryImageItem(
                                            image = image,
                                            isSelected = isSelected,
                                            isListMode = true,
                                            columnCount = 1,
                                            viewSettings = viewSettings,
                                            onThumbnailClick = {
                                                viewModel.toggleSelection(image.id)
                                            },
                                            onClick = {
                                                val flatIdx = state.flatImages.indexOfFirst { it.id == image.id }
                                                if (flatIdx >= 0) {
                                                    onImageClick(flatIdx)
                                                }
                                            },
                                            onLongClick = {
                                                viewModel.toggleSelection(image.id)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        } else {
                            var currentColumns by remember(gridCellsIndex) { mutableStateOf(4 - gridCellsIndex.coerceIn(0, 2)) }
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
                                    top = paddingValues.calculateTopPadding() + 8.dp,
                                    bottom = paddingValues.calculateBottomPadding() + 16.dp,
                                    start = 8.dp,
                                    end = 8.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
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
                                                            }
                                                            hasChangedInThisGesture = true
                                                        } else if (accumulatedZoom < 0.75f) {
                                                            val newCols = (currentColumns + 1).coerceIn(2, 4)
                                                            if (newCols != currentColumns) {
                                                                currentColumns = newCols
                                                                viewModel.setGridCellsIndex(4 - newCols)
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
                                state.grouped.forEach { (dateLabel, images) ->
                                    item(
                                        key = "header_$dateLabel",
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) {
                                        DateGroupHeader(label = dateLabel)
                                    }

                                    items(
                                        items = images,
                                        key = { it.id },
                                        contentType = { "gallery_image" }
                                    ) { image ->
                                        val isSelected = image.id in selectedIds
                                        GalleryImageItem(
                                            image = image,
                                            isSelected = isSelected,
                                            isListMode = false,
                                            columnCount = animatedColumns.coerceIn(2, 4),
                                            viewSettings = viewSettings,
                                            onClick = {
                                                if (selectedIds.isNotEmpty()) {
                                                    viewModel.toggleSelection(image.id)
                                                } else {
                                                    val flatIdx = state.flatImages.indexOfFirst { it.id == image.id }
                                                    if (flatIdx >= 0) {
                                                        onImageClick(flatIdx)
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                viewModel.toggleSelection(image.id)
                                            },
                                            modifier = Modifier
                                                .animateItem()
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    val galleryViewMode by viewModel.galleryViewMode.collectAsState()

        if (showSettingsSheet) {
            GalleryViewSettingsBottomSheet(
                layoutMode = layoutMode,
                onLayoutModeChange = { viewModel.setLayoutMode(it) },
                gridCellsIndex = gridCellsIndex,
                onGridCellsIndexChange = { viewModel.setGridCellsIndex(it) },
                viewSettings = viewSettings,
                onViewSettingsChange = { viewModel.updateViewSettings(it) },
                sortOption = "date_newest",
                onSortOptionChange = {},
                isRootFolderView = false,
                showFolderThumbnail = false,
                onShowFolderThumbnailChange = {},
                galleryViewMode = galleryViewMode,
                onGalleryViewModeChange = { mode ->
                    viewModel.setGalleryViewMode(mode)
                    if (mode == "albums") {
                        showSettingsSheet = false
                        onAlbumsClick()
                    }
                },
                onDismiss = { showSettingsSheet = false }
            )
        }

        if (showDetailsDialog) {
            val selectedImages = (uiState as? AllImagesState.Success)?.flatImages?.filter { it.id in selectedIds } ?: emptyList()
            DetailsDialog(
                selectedFolders = emptyList(),
                selectedImages = selectedImages,
                onDismiss = { showDetailsDialog = false }
            )
        }

        if (showRenameDialog) {
            val selectedId = selectedIds.firstOrNull()
            val image = (uiState as? AllImagesState.Success)?.flatImages?.find { it.id == selectedId }
            image?.let {
                CustomRenameDialog(
                    initialName = it.realPath.substringAfterLast('/'),
                    onConfirm = { newName ->
                        viewModel.renameSelectedImage(newName)
                        showRenameDialog = false
                    },
                    onDismiss = { showRenameDialog = false }
                )
            }
        }
    }

    if (showStorageExplorer) {
        val selectedImages = (uiState as? AllImagesState.Success)?.flatImages?.filter { it.id in selectedIds } ?: emptyList()
        com.devson.pixchive.ui.screens.StorageExplorerScreen(
            operationType = explorerOperationType,
            sourceUris = selectedImages.map { it.uri },
            onComplete = {
                showStorageExplorer = false
                viewModel.clearSelection()
            },
            onCancel = { showStorageExplorer = false }
        )
    }
    }
}

@Composable
private fun DateGroupHeader(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp
        )
    }
}
