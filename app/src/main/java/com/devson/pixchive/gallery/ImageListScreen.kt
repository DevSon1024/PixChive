package com.devson.pixchive.gallery

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.gallery.data.models.GalleryFolder
import com.devson.pixchive.gallery.ui.components.DetailsDialog
import com.devson.pixchive.gallery.ui.components.GallerySelectionBottomBar
import com.devson.pixchive.gallery.ui.components.GalleryFolderItem
import com.devson.pixchive.gallery.ui.components.GalleryViewSettingsBottomSheet
import com.devson.pixchive.gallery.viewmodel.GalleryState
import com.devson.pixchive.gallery.viewmodel.ImageListViewModel
import com.devson.pixchive.utils.PermissionHelper
import com.dokar.pinchzoomgrid.PinchZoomGridLayout
import com.dokar.pinchzoomgrid.rememberPinchZoomGridState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageListScreen(
    onNavigateBack: () -> Unit,
    onFolderClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: ImageListViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val savedGridCellsIndex by viewModel.gridCellsIndex.collectAsState()
    val layoutMode by viewModel.layoutMode.collectAsState()
    val viewSettings by viewModel.viewSettings.collectAsState()

    var hasPermission by remember { mutableStateOf(PermissionHelper.hasStoragePermission(context)) }

    val selectedFolderIds = remember { mutableStateListOf<String>() }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    val cellsConfig = remember {
        mapOf(
            GridCells.Fixed(4) to 4,
            GridCells.Fixed(3) to 3,
            GridCells.Fixed(2) to 2
        )
    }
    val cellsList = remember { cellsConfig.keys.toList() }

    val pinchZoomState = rememberPinchZoomGridState(
        cellsList = cellsList,
        initialCellsIndex = savedGridCellsIndex.coerceIn(0, cellsList.lastIndex)
    )

    BackHandler(enabled = selectedFolderIds.isNotEmpty()) {
        selectedFolderIds.clear()
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

    val selectedFolders: List<GalleryFolder> = if (showDetailsDialog) {
        (uiState as? GalleryState.Success)?.folders?.filter { it.bucketId in selectedFolderIds } ?: emptyList()
    } else emptyList()

    Scaffold(
        topBar = {
            if (selectedFolderIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text("${selectedFolderIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectedFolderIds.clear() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
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
                    onMove = {},
                    onCopy = {},
                    onDelete = {},
                    onRename = {},
                    onShare = {},
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
                                    contentPadding = PaddingValues(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    listItems(state.folders, key = { it.bucketId }) { folder ->
                                        GalleryFolderItem(
                                            folder = folder,
                                            isSelected = folder.bucketId in selectedFolderIds,
                                            isListMode = true,
                                            viewSettings = viewSettings,
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = {
                                                if (selectedFolderIds.isNotEmpty()) {
                                                    if (folder.bucketId in selectedFolderIds) {
                                                        selectedFolderIds.remove(folder.bucketId)
                                                    } else {
                                                        selectedFolderIds.add(folder.bucketId)
                                                    }
                                                } else {
                                                    onFolderClick(folder.bucketId)
                                                }
                                            },
                                            onLongPress = {
                                                if (folder.bucketId !in selectedFolderIds) {
                                                    selectedFolderIds.add(folder.bucketId)
                                                }
                                            }
                                        )
                                    }
                                }
                            } else {
                                PinchZoomGridLayout(state = pinchZoomState) {
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
                                        contentPadding = PaddingValues(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(state.folders, key = { it.bucketId }) { folder ->
                                            GalleryFolderItem(
                                                folder = folder,
                                                isSelected = folder.bucketId in selectedFolderIds,
                                                viewSettings = viewSettings,
                                                modifier = Modifier.pinchItem(key = folder.bucketId),
                                                onClick = {
                                                    if (selectedFolderIds.isNotEmpty()) {
                                                        if (folder.bucketId in selectedFolderIds) {
                                                            selectedFolderIds.remove(folder.bucketId)
                                                        } else {
                                                            selectedFolderIds.add(folder.bucketId)
                                                        }
                                                    } else {
                                                        onFolderClick(folder.bucketId)
                                                    }
                                                },
                                                onLongPress = {
                                                    if (folder.bucketId !in selectedFolderIds) {
                                                        selectedFolderIds.add(folder.bucketId)
                                                    }
                                                }
                                            )
                                        }
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
    }
}