package com.devson.pixchive.gallery

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.gallery.data.models.GalleryFolder
import com.devson.pixchive.gallery.ui.components.DetailsDialog
import com.devson.pixchive.gallery.ui.components.GalleryFolderItem
import com.devson.pixchive.gallery.ui.components.GallerySelectionBottomBar
import com.devson.pixchive.gallery.ui.components.GalleryViewSettingsBottomSheet
import com.devson.pixchive.gallery.ui.components.CustomRenameDialog
import com.devson.pixchive.gallery.viewmodel.GalleryState
import com.devson.pixchive.gallery.viewmodel.ImageListViewModel
import com.devson.pixchive.utils.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageListScreen(
    onNavigateBack: () -> Unit,
    onFolderClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onAllImagesClick: () -> Unit = {},
    viewModel: ImageListViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val savedGridCellsIndex by viewModel.gridCellsIndex.collectAsState()
    val layoutMode by viewModel.layoutMode.collectAsState()
    val viewSettings by viewModel.viewSettings.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val showFolderThumbnail by viewModel.showFolderThumbnail.collectAsState()
    val galleryViewMode by viewModel.galleryViewMode.collectAsState()

    var hasPermission by remember { mutableStateOf(PermissionHelper.hasStoragePermission(context)) }

    val selectedFolderIds by viewModel.selectedIds.collectAsState()
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()

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

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (isGranted) viewModel.loadGalleryFolders()
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
            if (selectedFolderIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text("${selectedFolderIds.size} selected") },
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
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            } else {
                TopAppBar(
                    title = { Text("Device Gallery") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate Back")
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
            if (selectedFolderIds.isNotEmpty()) {
                GallerySelectionBottomBar(
                    selectedCount = selectedFolderIds.size,
                    selectedImages = emptyList(),
                    onMove = {},
                    onCopy = {},
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
            if (!hasPermission) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(paddingValues)
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
                        text = "PixChive needs access to your storage to find device images.",
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
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
                            Text(
                                text = "No images found on this device.",
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            if (layoutMode == "list") {
                                LazyColumn(
                                    contentPadding = PaddingValues(
                                        top = paddingValues.calculateTopPadding() + 12.dp,
                                        bottom = paddingValues.calculateBottomPadding() + 16.dp,
                                        start = 12.dp,
                                        end = 12.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    listItems(state.folders, key = { it.bucketId }) { folder ->
                                        GalleryFolderItem(
                                            folder = folder,
                                            isSelected = folder.bucketId in selectedFolderIds,
                                            isListMode = true,
                                            viewSettings = viewSettings,
                                            showThumbnail = showFolderThumbnail,
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = { onFolderClick(folder.bucketId) },
                                            onThumbnailClick = { viewModel.toggleSelection(folder.bucketId) },
                                            onLongPress = { viewModel.toggleSelection(folder.bucketId) }
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
                                        top = paddingValues.calculateTopPadding() + 12.dp,
                                        bottom = paddingValues.calculateBottomPadding() + 16.dp,
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
                                    items(state.folders, key = { it.bucketId }) { folder ->
                                        GalleryFolderItem(
                                            folder = folder,
                                            isSelected = folder.bucketId in selectedFolderIds,
                                            gridColumns = animatedColumns.coerceIn(2, 4),
                                            viewSettings = viewSettings,
                                            showThumbnail = showFolderThumbnail,
                                            modifier = Modifier
                                                .animateItem()
                                                .fillMaxWidth(),
                                            onClick = {
                                                if (selectedFolderIds.isNotEmpty()) {
                                                    viewModel.toggleSelection(folder.bucketId)
                                                } else {
                                                    onFolderClick(folder.bucketId)
                                                }
                                            },
                                            onLongPress = { viewModel.toggleSelection(folder.bucketId) }
                                        )
                                    }
                                }
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
                isRootFolderView = true,
                showFolderThumbnail = showFolderThumbnail,
                onShowFolderThumbnailChange = { viewModel.setShowFolderThumbnail(it) },
                galleryViewMode = galleryViewMode,
                onGalleryViewModeChange = { mode ->
                    viewModel.setGalleryViewMode(mode)
                    if (mode == "all_images") {
                        showSettingsSheet = false
                        onAllImagesClick()
                    }
                },
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
            val selectedId = selectedFolderIds.firstOrNull()
            val folder = (uiState as? GalleryState.Success)?.folders?.find { it.bucketId == selectedId }
            folder?.let {
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