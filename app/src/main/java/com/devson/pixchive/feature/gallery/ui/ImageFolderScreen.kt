package com.devson.pixchive.feature.gallery.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed as listItemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.core.data.FileOperationsViewModel
import com.devson.pixchive.core.designsystem.component.PixChiveEmptyState
import com.devson.pixchive.core.designsystem.component.SkeletonLoadingView
import com.devson.pixchive.feature.gallery.ui.components.CustomRenameDialog
import com.devson.pixchive.feature.gallery.ui.components.DetailsDialog
import com.devson.pixchive.feature.gallery.ui.components.GalleryImageItem
import com.devson.pixchive.feature.gallery.ui.components.GallerySelectionBottomBar
import com.devson.pixchive.feature.gallery.ui.components.GalleryViewSettingsBottomSheet
import com.devson.pixchive.feature.gallery.ui.components.GlobalSearchAppBar
import com.devson.pixchive.feature.gallery.viewmodel.GalleryFolderViewModel
import com.devson.pixchive.feature.gallery.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageFolderScreen(
    bucketId: String,
    onNavigateBack: () -> Unit,
    onImageClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onSearch: (String) -> Unit = {},
    viewModel: GalleryFolderViewModel = viewModel()
) {
    val images by viewModel.images.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val savedGridCellsIndex by viewModel.gridCellsIndex.collectAsState()
    val layoutMode by viewModel.layoutMode.collectAsState()
    val viewSettings by viewModel.viewSettings.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()

    val searchViewModel: SearchViewModel = viewModel()
    val searchQuery by searchViewModel.searchQuery.collectAsState()
    val suggestions by searchViewModel.suggestions.collectAsState()

    val selectedImageIds by viewModel.selectedIds.collectAsState()
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

    val gridState = rememberLazyGridState()

    BackHandler(enabled = selectedImageIds.isNotEmpty()) {
        viewModel.clearSelection()
    }

    LaunchedEffect(bucketId) {
        viewModel.loadImages(bucketId)
    }

    val selectedImages = remember(selectedImageIds, images) {
        images.filter { it.id in selectedImageIds }
    }

    val folderName by viewModel.folderName.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (selectedImageIds.isNotEmpty()) {
                    TopAppBar(
                        title = { Text("${selectedImageIds.size} selected", fontWeight = FontWeight.Bold) },
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
                        title = folderName,
                        searchQuery = searchQuery,
                        suggestions = suggestions,
                        onQueryChange = { searchViewModel.updateSearchQuery(it) },
                        onSearch = onSearch,
                        onBackClick = onNavigateBack,
                        actions = {
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
                AnimatedVisibility(
                    visible = selectedImageIds.isNotEmpty(),
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
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
                        onDelete = {
                            fileOpsViewModel.deleteImages(context, selectedImages.map { it.uri }, trash = true)
                        },
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
                    .padding(paddingValues)
            ) {
                if (isLoading) {
                    val baseColumns = if (layoutMode == "list") 1 else (4 - savedGridCellsIndex.coerceIn(0, 2))
                    SkeletonLoadingView(
                        layoutMode = layoutMode,
                        columns = baseColumns
                    )
                } else if (images.isEmpty()) {
                    PixChiveEmptyState(
                        icon = Icons.Default.PhotoLibrary,
                        title = "Folder is Empty",
                        message = "No images found in this folder."
                    )
                } else {
                    if (layoutMode == "list") {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                top = 8.dp,
                                bottom = 100.dp
                            ),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            listItemsIndexed(images, key = { _, img -> img.id }) { index, image ->
                                GalleryImageItem(
                                    image = image,
                                    isSelected = image.id in selectedImageIds,
                                    isListMode = true,
                                    columnCount = 1,
                                    viewSettings = viewSettings,
                                    onThumbnailClick = { viewModel.toggleSelection(image.id) },
                                    onClick = {
                                        if (selectedImageIds.isNotEmpty()) {
                                            viewModel.toggleSelection(image.id)
                                        } else {
                                            onImageClick(index)
                                        }
                                    },
                                    onLongClick = { viewModel.toggleSelection(image.id) },
                                    modifier = Modifier.fillMaxWidth()
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
                                top = 8.dp,
                                bottom = 100.dp,
                                start = 4.dp,
                                end = 4.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
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
                            gridItemsIndexed(images, key = { _, img -> img.id }) { index, image ->
                                GalleryImageItem(
                                    image = image,
                                    isSelected = image.id in selectedImageIds,
                                    isListMode = false,
                                    columnCount = animatedColumns.coerceIn(2, 4),
                                    viewSettings = viewSettings,
                                    modifier = Modifier
                                        .animateItem()
                                        .fillMaxWidth()
                                        .aspectRatio(1f),
                                    onClick = {
                                        if (selectedImageIds.isNotEmpty()) {
                                            viewModel.toggleSelection(image.id)
                                        } else {
                                            onImageClick(index)
                                        }
                                    },
                                    onLongClick = { viewModel.toggleSelection(image.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSettingsSheet) {
            GalleryViewSettingsBottomSheet(
                layoutMode = layoutMode,
                onLayoutModeChange = { viewModel.setLayoutMode(it) },
                gridCellsIndex = savedGridCellsIndex,
                onGridCellsIndexChange = { viewModel.setGridCellsIndex(it) },
                viewSettings = viewSettings,
                onViewSettingsChange = { viewModel.updateViewSettings(it) },
                sortOption = sortOption,
                onSortOptionChange = { viewModel.setSortOption(it) },
                onDismiss = { showSettingsSheet = false }
            )
        }

        if (showDetailsDialog) {
            DetailsDialog(
                selectedFolders = emptyList(),
                selectedImages = selectedImages,
                onDismiss = { showDetailsDialog = false }
            )
        }

        if (showRenameDialog) {
            val selectedId = selectedImageIds.firstOrNull()
            val image = selectedImages.find { it.id == selectedId }
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

        if (showStorageExplorer) {
            StorageExplorerScreen(
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