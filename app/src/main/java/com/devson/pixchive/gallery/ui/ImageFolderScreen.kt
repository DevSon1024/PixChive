package com.devson.pixchive.gallery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed as listItemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.gallery.ui.components.DetailsDialog
import com.devson.pixchive.gallery.ui.components.GallerySelectionBottomBar
import com.devson.pixchive.gallery.ui.components.GalleryImageItem
import com.devson.pixchive.gallery.ui.components.GalleryViewSettingsBottomSheet
import com.devson.pixchive.gallery.ui.components.CustomRenameDialog
import com.devson.pixchive.gallery.viewmodel.GalleryFolderViewModel
import com.devson.pixchive.viewmodel.FileOperationsViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ImageFolderScreen(
    bucketId: String,
    onNavigateBack: () -> Unit,
    onImageClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: GalleryFolderViewModel = viewModel()
) {
    val images by viewModel.images.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val savedGridCellsIndex by viewModel.gridCellsIndex.collectAsState()
    val layoutMode by viewModel.layoutMode.collectAsState()
    val viewSettings by viewModel.viewSettings.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()

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

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            if (selectedImageIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text("${selectedImageIds.size} selected") },
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
                TopAppBar(
                    title = { Text("Folder Images") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "View Settings")
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "App Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (selectedImageIds.isNotEmpty()) {
                GallerySelectionBottomBar(
                    selectedImages = selectedImages,
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
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).padding(paddingValues))
            } else if (images.isEmpty()) {
                Text("No images found.", modifier = Modifier.align(Alignment.Center).padding(paddingValues))
            } else {
                if (layoutMode == "list") {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = paddingValues.calculateTopPadding(),
                            bottom = paddingValues.calculateBottomPadding() + 16.dp
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        listItemsIndexed(images, key = { _, img -> img.id }) { index, image ->
                            val sharedModifier = with(sharedTransitionScope) {
                                Modifier.sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "image_${image.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                            GalleryImageItem(
                                image = image,
                                isSelected = image.id in selectedImageIds,
                                isListMode = true,
                                columnCount = 1,
                                viewSettings = viewSettings,
                                onThumbnailClick = { viewModel.toggleSelection(image.id) },
                                onClick = { onImageClick(index) },
                                onLongClick = { viewModel.toggleSelection(image.id) },
                                modifier = sharedModifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    var currentColumns by remember(savedGridCellsIndex) {
                        mutableStateOf(4 - savedGridCellsIndex.coerceIn(0, 2))
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
                            top = paddingValues.calculateTopPadding() + 2.dp,
                            bottom = paddingValues.calculateBottomPadding() + 16.dp,
                            start = 2.dp,
                            end = 2.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
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
                            val sharedModifier = with(sharedTransitionScope) {
                                Modifier.sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "image_${image.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                            GalleryImageItem(
                                image = image,
                                isSelected = image.id in selectedImageIds,
                                isListMode = false,
                                columnCount = animatedColumns.coerceIn(2, 4),
                                viewSettings = viewSettings,
                                modifier = sharedModifier
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
            val image = images.find { it.id == selectedId }
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
        val selectedImages = images.filter { it.id in selectedImageIds }
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