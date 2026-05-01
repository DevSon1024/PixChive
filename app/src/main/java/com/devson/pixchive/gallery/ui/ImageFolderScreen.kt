package com.devson.pixchive.gallery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed as listItemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.gallery.data.models.GalleryImage
import com.devson.pixchive.gallery.ui.components.DetailsDialog
import com.devson.pixchive.gallery.ui.components.GallerySelectionBottomBar
import com.devson.pixchive.gallery.ui.components.GalleryImageItem
import com.devson.pixchive.gallery.ui.components.GalleryViewSettingsBottomSheet
import com.devson.pixchive.gallery.viewmodel.GalleryFolderViewModel
import com.dokar.pinchzoomgrid.PinchZoomGridLayout
import com.dokar.pinchzoomgrid.rememberPinchZoomGridState

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

    val selectedImageIds = remember { mutableStateListOf<Long>() }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    // 4 cols (zoom out) -> 3 -> 2 (zoom in)
    val cellsConfig = remember {
        mapOf(
            GridCells.Fixed(4) to 4,
            GridCells.Fixed(3) to 3,
            GridCells.Fixed(2) to 2
        )
    }
    val cellsList = remember { cellsConfig.keys.toList() }

    val pinchZoomGridState = rememberPinchZoomGridState(
        cellsList = cellsList,
        initialCellsIndex = savedGridCellsIndex.coerceIn(0, cellsList.lastIndex)
    )

    BackHandler(enabled = selectedImageIds.isNotEmpty()) {
        selectedImageIds.clear()
    }

    LaunchedEffect(bucketId) {
        viewModel.loadImages(bucketId)
    }

    val selectedImages: List<GalleryImage> = if (showDetailsDialog) {
        images.filter { it.id in selectedImageIds }
    } else emptyList()

    Scaffold(
        topBar = {
            if (selectedImageIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text("${selectedImageIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectedImageIds.clear() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    }
                )
            }
        },
        bottomBar = {
            if (selectedImageIds.isNotEmpty()) {
                GallerySelectionBottomBar(
                    selectedCount = selectedImageIds.size,
                    onMove = {},
                    onCopy = {},
                    onDelete = {},
                    onRename = {},
                    onShare = {},
                    onInfo = { showDetailsDialog = true }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (images.isEmpty()) {
                Text("No images found.", modifier = Modifier.align(Alignment.Center))
            } else {
                if (layoutMode == "list") {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp),
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
                                modifier = sharedModifier.fillMaxWidth(),
                                onClick = {
                                    if (selectedImageIds.isNotEmpty()) {
                                        if (image.id in selectedImageIds) selectedImageIds.remove(image.id)
                                        else selectedImageIds.add(image.id)
                                    } else {
                                        onImageClick(index)
                                    }
                                },
                                onLongClick = {
                                    if (image.id !in selectedImageIds) selectedImageIds.add(image.id)
                                }
                            )
                        }
                    }
                } else {
                    PinchZoomGridLayout(state = pinchZoomGridState) {
                        // gridCells and gridState are provided by PinchZoomGridLayout scope
                        val currentColumnCount = cellsConfig[gridCells] ?: 4

                        LaunchedEffect(currentColumnCount) {
                            val newIndex = 4 - currentColumnCount
                            if (newIndex in 0..2 && newIndex != savedGridCellsIndex) {
                                viewModel.setGridCellsIndex(newIndex)
                            }
                        }

                        LazyVerticalGrid(
                            columns = gridCells,
                            state = gridState,
                            contentPadding = PaddingValues(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxSize()
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
                                    columnCount = currentColumnCount,
                                    viewSettings = viewSettings,
                                    modifier = sharedModifier.pinchItem(key = image.id),
                                    onClick = {
                                        if (selectedImageIds.isNotEmpty()) {
                                            if (image.id in selectedImageIds) selectedImageIds.remove(image.id)
                                            else selectedImageIds.add(image.id)
                                        } else {
                                            onImageClick(index)
                                        }
                                    },
                                    onLongClick = {
                                        if (image.id !in selectedImageIds) selectedImageIds.add(image.id)
                                    }
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
    }
}