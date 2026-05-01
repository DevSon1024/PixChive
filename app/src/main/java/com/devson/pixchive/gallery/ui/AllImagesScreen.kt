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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.gallery.ui.components.GalleryImageItem
import com.devson.pixchive.gallery.ui.components.GallerySelectionBottomBar
import com.devson.pixchive.gallery.ui.components.GalleryViewSettingsBottomSheet
import com.devson.pixchive.gallery.viewmodel.AllImagesState
import com.devson.pixchive.gallery.viewmodel.AllImagesViewModel



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllImagesScreen(
    onNavigateBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onImageClick: (Int) -> Unit = {},
    onAlbumsClick: () -> Unit = {},
    viewModel: AllImagesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val layoutMode by viewModel.layoutMode.collectAsState()
    val gridCellsIndex by viewModel.gridCellsIndex.collectAsState()
    val viewSettings by viewModel.viewSettings.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }



    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = 0
    )

    BackHandler(enabled = selectedIds.isNotEmpty()) {
        viewModel.clearSelection()
    }

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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            } else {
                TopAppBar(
                    title = { Text("All Images") },
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
            if (selectedIds.isNotEmpty()) {
                GallerySelectionBottomBar(
                    selectedCount = selectedIds.size,
                    onMove = {},
                    onCopy = {},
                    onDelete = {},
                    onRename = {},
                    onShare = {},
                    onInfo = {}
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
            when (val state = uiState) {
                is AllImagesState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is AllImagesState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
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
                                contentPadding = PaddingValues(0.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalArrangement = Arrangement.Top,
                                modifier = Modifier.fillMaxSize()
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
                                        key = { it.id }
                                    ) { image ->
                                        val isSelected = image.id in selectedIds
                                        GalleryImageItem(
                                            image = image,
                                            isSelected = isSelected,
                                            isListMode = true,
                                            columnCount = 1,
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
                                                if (selectedIds.isEmpty()) {
                                                    viewModel.enterSelectionMode(image.id)
                                                } else {
                                                    viewModel.toggleSelection(image.id)
                                                }
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
                                contentPadding = PaddingValues(8.dp),
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
                                        key = { it.id }
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
                                                if (selectedIds.isEmpty()) {
                                                    viewModel.enterSelectionMode(image.id)
                                                } else {
                                                    viewModel.toggleSelection(image.id)
                                                }
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
