package com.devson.pixchive.ui.screens.imagelist

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.data.local.ImageEntity
import com.devson.pixchive.data.ComicFolder
import com.devson.pixchive.model.ViewMode
import com.devson.pixchive.model.WatchHistory
import com.devson.pixchive.model.applySort
import com.devson.pixchive.ui.components.CustomRenameDialog
import com.devson.pixchive.ui.components.PreviewFloatingActionButton
import com.devson.pixchive.ui.components.ViewSettingsBottomSheet
import com.devson.pixchive.ui.screens.imagelist.components.folder.FolderListContent
import com.devson.pixchive.ui.screens.InformationBottomSheet
import com.devson.pixchive.ui.screens.StorageExplorerScreen
import com.devson.pixchive.ui.screens.imagelist.components.topbar.ImageEntityListTopAppBar
import com.devson.pixchive.ui.screens.imagelist.components.list.ImageEntityListContent
import com.devson.pixchive.ui.screens.imagelist.components.explorer.ExplorerListContent
import com.devson.pixchive.ui.screens.imagelist.components.selection.ImageEntitySelectionBottomBar
import com.devson.pixchive.ui.screens.imagelist.utils.applyFolderSort
import com.devson.pixchive.ui.screens.imagelist.utils.shareImageEntitys
import com.devson.pixchive.util.SelectionBottomAppBar
import com.devson.pixchive.viewmodel.FileOperationsViewModel
import com.devson.pixchive.viewmodel.HomeViewModel
import com.devson.pixchive.viewmodel.FolderViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FolderListScreen(
    onImageEntitySelected: (ImageEntity, List<ImageEntity>, Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onBack: () -> Unit = {},
    onNavigateToSearch: (String) -> Unit = {},
    viewModel: FolderViewModel = viewModel()
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            viewModel.loadImageEntitys()
        }
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_VIDEO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            permissionLauncher.launch(permission)
        } else {
            viewModel.loadImageEntitys()
        }
    }

    val imagesByFolder by viewModel.imagesByFolder.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val viewSettings by viewModel.viewSettings.collectAsState()
    val explorerNodes by viewModel.explorerNodes.collectAsState()
    val currentExplorerPath by viewModel.currentExplorerPath.collectAsState()

    val searchSuggestions by viewModel.searchSuggestions.collectAsState()
    var searchActive by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(searchActive) {
        if (searchActive) searchFocusRequester.requestFocus()
    }

    var showSettingsSheet by remember { mutableStateOf(false) }

    //  SELECTION STATE 
    var selectedFolders by remember { mutableStateOf(emptySet<ComicFolder>()) }
    var selectedImageEntitys by remember { mutableStateOf(emptySet<ImageEntity>()) }
    var showInfoBottomSheet by remember { mutableStateOf(false) }

    val sortedFolderKeys = remember(imagesByFolder, viewSettings.sortField, viewSettings.sortDirection) {
        val keys = imagesByFolder.keys.toList()
        keys.applyFolderSort(imagesByFolder, viewSettings.sortField, viewSettings.sortDirection)
    }
    val isSelectionActive = selectedFolders.isNotEmpty() || selectedImageEntitys.isNotEmpty()

    // Hoisted scroll states - survive recomposition and view-mode toggling
    val folderListState = rememberLazyListState()
    val folderGridState = rememberLazyGridState()
    var currentFolderId by rememberSaveable { mutableStateOf<String?>(null) }
    val imageListState = rememberLazyListState()
    val imageGridState = rememberLazyGridState()

    // Reset image scroll position when entering a different folder
    LaunchedEffect(selectedFolder) {
        if (selectedFolder?.name != currentFolderId) {
            imageListState.scrollToItem(0)
            imageGridState.scrollToItem(0)
            currentFolderId = selectedFolder?.name
        }
    }

    //  Watch History 
    val homeViewModel: HomeViewModel = viewModel()
    val history by homeViewModel.history.collectAsState()
    val historyMap = remember(history) { history.associateBy { it.uri } }

    //  File Operations 
    val fileOpsViewModel: FileOperationsViewModel = viewModel()

    // Handles MediaStore permission dialogs (delete/rename on API 29-30)
    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fileOpsViewModel.onPermissionGranted(context)
        }
        fileOpsViewModel.clearPendingIntentSender()
    }

    //  Storage Explorer State 
    var storageExplorerOp by remember { mutableStateOf<String?>(null) }
    var storageExplorerUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    if (storageExplorerOp != null) {
        BackHandler { storageExplorerOp = null }
        StorageExplorerScreen(
            operationType = storageExplorerOp!!,
            sourceUris = storageExplorerUris,
            onComplete = {
                storageExplorerOp = null
                selectedFolders = emptySet()
                selectedImageEntitys = emptySet()
            },
            onCancel = {
                storageExplorerOp = null
            }
        )
        return
    }

    //  Dialog state 
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInputText by remember { mutableStateOf("") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    //  State change callbacks → MainScreen 
    //  Watch pendingIntentSender and launch it automatically 
    val pendingIntentSender by fileOpsViewModel.pendingIntentSender.collectAsState()
    LaunchedEffect(pendingIntentSender) {
        pendingIntentSender?.let { sender ->
            intentSenderLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

    //  File operation result → Toast + list reload 
    val opResult by fileOpsViewModel.operationResult.collectAsState()
    LaunchedEffect(opResult) {
        opResult?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.loadImageEntitys()
            fileOpsViewModel.clearResult()
        }
    }

    val needsRefresh by fileOpsViewModel.needsRefresh.collectAsState()
    LaunchedEffect(needsRefresh) {
        if (needsRefresh) {
            viewModel.loadImageEntitys(forceRefresh = true)
            fileOpsViewModel.onRefreshHandled()
        }
    }

    // Drives progress bar visibility
    val opInProgress by fileOpsViewModel.operationInProgress.collectAsState()

    // Back handler: clears selection first before navigating out
    BackHandler(enabled = selectedFolder != null || isSelectionActive || (viewSettings.viewMode == ViewMode.FOLDERS && currentExplorerPath != null)) {
        when {
            selectedImageEntitys.isNotEmpty() -> selectedImageEntitys = emptySet()
            selectedFolders.isNotEmpty() -> selectedFolders = emptySet()
            selectedFolder != null -> viewModel.selectFolder(null)
            viewSettings.viewMode == ViewMode.FOLDERS && currentExplorerPath != null -> viewModel.navigateExplorerUp()
            else -> {}
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            val titleText = when (viewSettings.viewMode) {
                ViewMode.ALL_FOLDERS -> selectedFolder?.name
                ViewMode.FILES -> "All Files"
                ViewMode.FOLDERS -> currentExplorerPath?.substringBeforeLast('/')?.substringAfterLast('/') ?: "Folders"
            }
            ImageEntityListTopAppBar(
                isSelectionActive = isSelectionActive,
                titleText = titleText,
                selectedCount = selectedImageEntitys.size + selectedFolders.size,
                totalCount = when (viewSettings.viewMode) {
                    ViewMode.ALL_FOLDERS -> if (selectedFolder != null) (imagesByFolder[selectedFolder] ?: emptyList()).size else sortedFolderKeys.size
                    ViewMode.FILES -> imagesByFolder.values.flatten().size
                    ViewMode.FOLDERS -> explorerNodes.first.size + explorerNodes.second.size
                },
                showBackButton = selectedFolder != null || (viewSettings.viewMode == ViewMode.FOLDERS && currentExplorerPath != null),
                onClearSelection = { 
                    selectedFolders = emptySet()
                    selectedImageEntitys = emptySet()
                },
                onSelectAll = {
                    when (viewSettings.viewMode) {
                        ViewMode.ALL_FOLDERS -> {
                            if (selectedFolder != null) {
                                val allImageEntitys = imagesByFolder[selectedFolder] ?: emptyList()
                                selectedImageEntitys = if (selectedImageEntitys.size == allImageEntitys.size) emptySet() else allImageEntitys.toSet()
                            } else {
                                selectedFolders = if (selectedFolders.size == sortedFolderKeys.size) emptySet() else sortedFolderKeys.toSet()
                            }
                        }
                        ViewMode.FILES -> {
                            val allImageEntitys = imagesByFolder.values.flatten()
                            selectedImageEntitys = if (selectedImageEntitys.size == allImageEntitys.size) emptySet() else allImageEntitys.toSet()
                        }
                        ViewMode.FOLDERS -> {
                            val allExpImageEntitys = explorerNodes.second
                            val allExpFolders = explorerNodes.first
                            if (selectedImageEntitys.size + selectedFolders.size == allExpImageEntitys.size + allExpFolders.size) {
                                selectedImageEntitys = emptySet()
                                selectedFolders = emptySet()
                            } else {
                                selectedImageEntitys = allExpImageEntitys.toSet()
                                selectedFolders = allExpFolders.toSet()
                            }
                        }
                    }
                },
                onBack = onBack,
                onNavigateToSettings = onNavigateToSettings,
                onShowSettings = { showSettingsSheet = true },
                onSearch = onNavigateToSearch,
                searchActive = searchActive,
                searchText = searchText,
                onSearchActiveChange = { active ->
                    searchActive = active
                    if (!active) { searchText = ""; viewModel.clearSearch() }
                },
                onSearchTextChange = { text ->
                    searchText = text
                    viewModel.onSearchQueryChanged(text)
                },
                searchSuggestions = searchSuggestions,
                searchFocusRequester = searchFocusRequester,
                keyboard = keyboard,
                onBackToFolders = { 
                    if (viewSettings.viewMode == ViewMode.FOLDERS && currentExplorerPath != null) {
                        viewModel.navigateExplorerUp()
                    } else {
                        viewModel.selectFolder(null)
                    }
                }
            )
        },
        bottomBar = {
            if (isSelectionActive) {
                // Unified URI computation across all view modes
                val allImageEntitysFlat = remember(imagesByFolder) { imagesByFolder.values.flatten() }
                val selectedUris: List<Uri> = remember(
                    viewSettings.viewMode, selectedImageEntitys, selectedFolders, selectedFolder, imagesByFolder
                ) {
                    when (viewSettings.viewMode) {
                        ViewMode.FILES -> {
                            selectedImageEntitys.mapNotNull { runCatching { Uri.parse(it.uri) }.getOrNull() }
                        }
                        ViewMode.ALL_FOLDERS -> {
                            if (selectedFolder != null) {
                                // Inside a folder: operate on selected individual images
                                selectedImageEntitys.mapNotNull { runCatching { Uri.parse(it.uri) }.getOrNull() }
                            } else {
                                // Folder-list view: map every image inside selected folders
                                selectedFolders
                                    .flatMap { folder -> imagesByFolder[folder] ?: emptyList() }
                                    .mapNotNull { runCatching { Uri.parse(it.uri) }.getOrNull() }
                            }
                        }
                        ViewMode.FOLDERS -> {
                            // Combine standalone selected images AND all images inside selected dirs
                            val fromFolders = selectedFolders
                                .flatMap { f -> allImageEntitysFlat.filter { it.path.startsWith(f.id) } }
                            (selectedImageEntitys.toList() + fromFolders)
                                .distinctBy { it.uri }
                                .mapNotNull { runCatching { Uri.parse(it.uri) }.getOrNull() }
                        }
                    }
                }

                // In ALL_FOLDERS folder-list view, keep the original SelectionBottomAppBar
                // for its folder-centric features (Play All folder, Rename folder, etc.)
                val useFolderBar = viewSettings.viewMode == ViewMode.ALL_FOLDERS
                    && selectedFolder == null
                    && selectedFolders.isNotEmpty()

                if (useFolderBar) {
                    SelectionBottomAppBar(
                        selectedFolders = selectedFolders,
                        imagesByFolder = imagesByFolder,
                        viewSettings = viewSettings,
                        onImageEntitySelected = { image, playlist ->
                            onImageEntitySelected(image, playlist, historyMap[image.uri]?.lastPositionMs ?: 0L)
                        },
                        onClearSelection = { selectedFolders = emptySet() },
                        onMove = {
                            if (selectedUris.isNotEmpty()) {
                                storageExplorerUris = selectedUris
                                storageExplorerOp = "MOVE"
                            }
                        },
                        onCopy = {
                            if (selectedUris.isNotEmpty()) {
                                storageExplorerUris = selectedUris
                                storageExplorerOp = "COPY"
                            }
                        },
                        onDelete = {
                            if (selectedUris.isNotEmpty()) {
                                showDeleteConfirmation = true
                            }
                        },
                        onRename = {
                            val folder = selectedFolders.first()
                            renameInputText = folder.name
                            showRenameDialog = true
                        },
                        onShare = {
                            val images = selectedFolders.flatMap { imagesByFolder[it] ?: emptyList() }
                            shareImageEntitys(context, images)
                            selectedFolders = emptySet()
                            selectedImageEntitys = emptySet()
                        },
                        onShowInfo = { showInfoBottomSheet = true },
                        onMarkStatus = { status ->
                            selectedFolders.flatMap { imagesByFolder[it] ?: emptyList() }.forEach { image ->
                                val position = when(status) {
                                    "NEW" -> 0L
                                    "RUNNING" -> image.duration / 2
                                    "ENDED" -> image.duration
                                    else -> 0L
                                }
                                homeViewModel.setWatchStatus(image, position)
                            }
                            selectedFolders = emptySet()
                            selectedImageEntitys = emptySet()
                        }
                    )
                } else {
                    // FILES, FOLDERS mode, or ALL_FOLDERS inside a specific folder:
                    // use the image-centric bar driven by the unified selectedUris list
                    ImageEntitySelectionBottomBar(
                        selectedImageEntitys = selectedImageEntitys,
                        onPlayAll = {
                            val playImageEntity = selectedImageEntitys.firstOrNull()
                            if (playImageEntity != null) {
                                val playlist = when (viewSettings.viewMode) {
                                    ViewMode.FILES -> imagesByFolder.values.flatten().applySort(viewSettings.sortField, viewSettings.sortDirection)
                                    ViewMode.ALL_FOLDERS -> (imagesByFolder[selectedFolder] ?: emptyList()).applySort(viewSettings.sortField, viewSettings.sortDirection)
                                    ViewMode.FOLDERS -> explorerNodes.second.applySort(viewSettings.sortField, viewSettings.sortDirection)
                                }
                                onImageEntitySelected(playImageEntity, playlist, historyMap[playImageEntity.uri]?.lastPositionMs ?: 0L)
                                selectedImageEntitys = emptySet()
                            }
                        },
                        onMove = {
                            if (selectedUris.isNotEmpty()) {
                                storageExplorerUris = selectedUris
                                storageExplorerOp = "MOVE"
                            }
                        },
                        onCopy = {
                            if (selectedUris.isNotEmpty()) {
                                storageExplorerUris = selectedUris
                                storageExplorerOp = "COPY"
                            }
                        },
                        onDelete = {
                            if (selectedUris.isNotEmpty()) {
                                showDeleteConfirmation = true
                            }
                        },
                        onRename = {
                            val image = selectedImageEntitys.firstOrNull()
                            if (image != null) {
                                renameInputText = image.title.substringBeforeLast(".")
                                showRenameDialog = true
                            }
                        },
                        onShare = {
                            shareImageEntitys(context, selectedImageEntitys.toList())
                            selectedImageEntitys = emptySet()
                            selectedFolders = emptySet()
                        },
                        onShowInfo = { showInfoBottomSheet = true },
                        onMarkStatus = { status ->
                            selectedImageEntitys.forEach { image ->
                                val position = when(status) {
                                    "NEW" -> 0L
                                    "RUNNING" -> image.duration / 2
                                    "ENDED" -> image.duration
                                    else -> 0L
                                }
                                homeViewModel.setWatchStatus(image, position)
                            }
                            selectedImageEntitys = emptySet()
                            selectedFolders = emptySet()
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (viewSettings.showFloatingButton && !isSelectionActive) {
                val allImageEntitysFlat = remember(imagesByFolder) { imagesByFolder.values.flatten() }

                val lastPlayedImageEntity = remember(history, selectedFolder, viewSettings.viewMode, currentExplorerPath, allImageEntitysFlat) {
                    if (viewSettings.viewMode == ViewMode.ALL_FOLDERS && selectedFolder != null) {
                        val folderImageEntitys = imagesByFolder[selectedFolder] ?: emptyList()
                        val folderUris = folderImageEntitys.map { it.uri }.toSet()
                        val lastHistory = history.firstOrNull { it.uri in folderUris }
                        if (lastHistory != null) folderImageEntitys.find { it.uri == lastHistory.uri } else null
                    } else if (viewSettings.viewMode == ViewMode.FOLDERS && currentExplorerPath != null) {
                        val pathImageEntitys = allImageEntitysFlat.filter { it.path.startsWith(currentExplorerPath!!) }
                        val pathUris = pathImageEntitys.map { it.uri }.toSet()
                        val lastHistory = history.firstOrNull { it.uri in pathUris }
                        if (lastHistory != null) pathImageEntitys.find { it.uri == lastHistory.uri } else null
                    } else {
                        val lastHistory = history.firstOrNull()
                        if (lastHistory != null) allImageEntitysFlat.find { it.uri == lastHistory.uri } else null
                    }
                }

                if (lastPlayedImageEntity != null) {
                    val lastHistoryEntry = remember(lastPlayedImageEntity, historyMap) { historyMap[lastPlayedImageEntity.uri] }
                    PreviewFloatingActionButton(
                        enablePreview = viewSettings.enableFabPreview,
                        previewUri = lastPlayedImageEntity.uri,
                        previewTitle = lastPlayedImageEntity.title,
                        previewDurationMs = lastPlayedImageEntity.duration,
                        previewLastPositionMs = lastHistoryEntry?.lastPositionMs ?: 0L,
                        onPlay = {
                            val playlist = when (viewSettings.viewMode) {
                                ViewMode.FILES -> allImageEntitysFlat.applySort(viewSettings.sortField, viewSettings.sortDirection)
                                ViewMode.ALL_FOLDERS -> if (selectedFolder != null) {
                                    (imagesByFolder[selectedFolder] ?: emptyList()).applySort(viewSettings.sortField, viewSettings.sortDirection)
                                } else {
                                    allImageEntitysFlat.applySort(viewSettings.sortField, viewSettings.sortDirection)
                                }
                                ViewMode.FOLDERS -> if (currentExplorerPath != null) {
                                    allImageEntitysFlat.filter { it.path.startsWith(currentExplorerPath!!) }.applySort(viewSettings.sortField, viewSettings.sortDirection)
                                } else {
                                    allImageEntitysFlat.applySort(viewSettings.sortField, viewSettings.sortDirection)
                                }
                            }
                            onImageEntitySelected(lastPlayedImageEntity, playlist, lastHistoryEntry?.lastPositionMs ?: 0L)
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Progress bar shown at top while a file operation is running
            if (opInProgress) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = padding.calculateTopPadding())
                        .align(Alignment.TopCenter)
                )
            }

            if (!hasPermission) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Storage permission is required to find images.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_VIDEO
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                        permissionLauncher.launch(permission)
                    }) {
                        Text("Grant Permission")
                    }
                }
            } else if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.loadImageEntitys(forceRefresh = true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (viewSettings.viewMode) {
                        ViewMode.ALL_FOLDERS -> {
                            if (selectedFolder == null) {
                                FolderListContent(
                                    folders = imagesByFolder,
                                    settings = viewSettings,
                                    selectedFolders = selectedFolders,
                                    historyMap = historyMap,
                                    onFolderClick = { folder ->
                                        if (isSelectionActive) {
                                            selectedFolders =
                                                if (folder in selectedFolders) selectedFolders - folder else selectedFolders + folder
                                        } else {
                                            viewModel.selectFolder(folder)
                                        }
                                    },
                                    onFolderLongClick = { folder ->
                                        selectedFolders =
                                            if (folder in selectedFolders) selectedFolders - folder else selectedFolders + folder
                                    },
                                    listState = folderListState,
                                    gridState = folderGridState,
                                    contentPadding = padding
                                )
                            } else {
                                val images = imagesByFolder[selectedFolder] ?: emptyList()
                                val sortedImageEntitys = remember(images, viewSettings.sortField, viewSettings.sortDirection) {
                                    images.applySort(viewSettings.sortField, viewSettings.sortDirection)
                                }
                                ImageEntityListContent(
                                    images = sortedImageEntitys,
                                    settings = viewSettings,
                                    selectedImageEntitys = selectedImageEntitys,
                                    onImageEntityClick = { image ->
                                        if (isSelectionActive) {
                                            selectedImageEntitys = if (image in selectedImageEntitys) selectedImageEntitys - image else selectedImageEntitys + image
                                        } else {
                                            onImageEntitySelected(image, sortedImageEntitys, historyMap[image.uri]?.lastPositionMs ?: 0L)
                                        }
                                    },
                                    onImageEntityLongClick = { image ->
                                        selectedImageEntitys = if (image in selectedImageEntitys) selectedImageEntitys - image else selectedImageEntitys + image
                                    },
                                    listState = imageListState,
                                    gridState = imageGridState,
                                    historyMap = historyMap,
                                    contentPadding = padding
                                )
                            }
                        }
                        ViewMode.FILES -> {
                            val allImageEntitys = remember(imagesByFolder) { imagesByFolder.values.flatten() }
                            val sortedImageEntitys = remember(allImageEntitys, viewSettings.sortField, viewSettings.sortDirection) {
                                allImageEntitys.applySort(viewSettings.sortField, viewSettings.sortDirection)
                            }
                            ImageEntityListContent(
                                images = sortedImageEntitys,
                                settings = viewSettings,
                                selectedImageEntitys = selectedImageEntitys,
                                onImageEntityClick = { image ->
                                    if (isSelectionActive) {
                                        selectedImageEntitys = if (image in selectedImageEntitys) selectedImageEntitys - image else selectedImageEntitys + image
                                    } else {
                                        onImageEntitySelected(image, sortedImageEntitys, historyMap[image.uri]?.lastPositionMs ?: 0L)
                                    }
                                },
                                onImageEntityLongClick = { image ->
                                    selectedImageEntitys = if (image in selectedImageEntitys) selectedImageEntitys - image else selectedImageEntitys + image
                                },
                                listState = imageListState,
                                gridState = imageGridState,
                                historyMap = historyMap,
                                contentPadding = padding
                            )
                        }
                        ViewMode.FOLDERS -> {
                            val (expFolders, expImageEntitys) = explorerNodes
                            val sortedExpImageEntitys = remember(expImageEntitys, viewSettings.sortField, viewSettings.sortDirection) {
                                expImageEntitys.applySort(viewSettings.sortField, viewSettings.sortDirection)
                            }
                            // Need a map to resolve explorer nodes content
                            val mappedImageEntitysByFolder = remember(imagesByFolder, expFolders) {
                                val allImageEntitys = imagesByFolder.values.flatten()
                                expFolders.associateWith { folder ->
                                    allImageEntitys.filter { it.path.startsWith(folder.id) }
                                }
                            }
                            val sortedExpFolders = remember(expFolders, viewSettings.sortField, viewSettings.sortDirection, mappedImageEntitysByFolder) {
                                expFolders.applyFolderSort(mappedImageEntitysByFolder, viewSettings.sortField, viewSettings.sortDirection)
                            }
                            val allImageEntitysForSize = remember(imagesByFolder) { imagesByFolder.values.flatten() }

                            ExplorerListContent(
                                folders = sortedExpFolders,
                                images = sortedExpImageEntitys,
                                allImageEntitysForSize = allImageEntitysForSize,
                                settings = viewSettings,
                                selectedFolders = selectedFolders,
                                selectedImageEntitys = selectedImageEntitys,
                                historyMap = historyMap,
                                onFolderClick = { folder ->
                                    if (isSelectionActive) {
                                        selectedFolders = if (folder in selectedFolders) selectedFolders - folder else selectedFolders + folder
                                    } else {
                                        viewModel.navigateToExplorerPath(folder.id)
                                    }
                                },
                                onFolderLongClick = { folder ->
                                    selectedFolders = if (folder in selectedFolders) selectedFolders - folder else selectedFolders + folder
                                },
                                onImageEntityClick = { image ->
                                    if (isSelectionActive) {
                                        selectedImageEntitys = if (image in selectedImageEntitys) selectedImageEntitys - image else selectedImageEntitys + image
                                    } else {
                                        onImageEntitySelected(image, sortedExpImageEntitys, historyMap[image.uri]?.lastPositionMs ?: 0L)
                                    }
                                },
                                onImageEntityLongClick = { image ->
                                    selectedImageEntitys = if (image in selectedImageEntitys) selectedImageEntitys - image else selectedImageEntitys + image
                                },
                                listState = folderListState,
                                gridState = folderGridState,
                                contentPadding = padding
                            )
                        }
                    }
                }
            }
        }
    }

    // ---- INFORMATION BOTTOM SHEET ----
    if (showInfoBottomSheet && isSelectionActive) {
        val imagesToShow = when (viewSettings.viewMode) {
            ViewMode.FILES -> selectedImageEntitys
            ViewMode.ALL_FOLDERS -> {
                if (selectedFolder != null) {
                    selectedImageEntitys
                } else {
                    selectedFolders.flatMap { imagesByFolder[it] ?: emptyList() }.toSet()
                }
            }
            ViewMode.FOLDERS -> {
                val allImageEntitysFlat = imagesByFolder.values.flatten()
                val fromFolders = selectedFolders.flatMap { f -> 
                    allImageEntitysFlat.filter { it.path.startsWith(f.id) } 
                }
                (selectedImageEntitys + fromFolders).toSet()
            }
        }
        InformationBottomSheet(
            selectedImageEntitys = imagesToShow,
            onDismiss = { showInfoBottomSheet = false }
        )
    }

    // ---- RENAME DIALOG ----
    if (showRenameDialog && (selectedFolders.size == 1 || selectedImageEntitys.size == 1)) {
        val isFolder = selectedFolders.size == 1 && selectedFolder == null
        val title = if (isFolder) "Rename Folder" else "Rename ImageEntity"
        
        CustomRenameDialog(
            initialName = renameInputText,
            title = title,
            onConfirm = { newName ->
                if (isFolder) {
                    val folder = selectedFolders.first()
                    val folderPath = if (folder.id.startsWith("/")) {
                        folder.id
                    } else {
                        (imagesByFolder[folder] ?: emptyList()).firstOrNull()?.path?.substringBeforeLast("/")
                    }
                    if (folderPath != null) {
                        fileOpsViewModel.renameFolder(context, folderPath, newName)
                    } else {
                        Toast.makeText(context, "Could not determine folder path.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val image = if (selectedFolder != null) selectedImageEntitys.firstOrNull() 
                               else (imagesByFolder[selectedFolders.first()] ?: emptyList()).firstOrNull()
                    if (image != null) {
                        fileOpsViewModel.renameImageEntity(context, Uri.parse(image.uri), newName)
                    }
                }
                showRenameDialog = false
                selectedFolders = emptySet()
                selectedImageEntitys = emptySet()
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    if (showSettingsSheet) {
        ViewSettingsBottomSheet(
            settings = viewSettings,
            isFolderView = selectedFolder == null,
            onDismiss = { showSettingsSheet = false },
            viewModel = viewModel
        )
    }

    if (showDeleteConfirmation) {
        val isFolder = viewSettings.viewMode == ViewMode.ALL_FOLDERS && selectedFolder == null && selectedFolders.isNotEmpty()
        val selectedUrisForDelete: List<Uri> = when (viewSettings.viewMode) {
            ViewMode.FILES -> selectedImageEntitys.mapNotNull { runCatching { Uri.parse(it.uri) }.getOrNull() }
            ViewMode.ALL_FOLDERS -> {
                if (selectedFolder != null) {
                    selectedImageEntitys.mapNotNull { runCatching { Uri.parse(it.uri) }.getOrNull() }
                } else {
                    selectedFolders.flatMap { folder -> imagesByFolder[folder] ?: emptyList() }
                        .mapNotNull { runCatching { Uri.parse(it.uri) }.getOrNull() }
                }
            }
            ViewMode.FOLDERS -> {
                val allImageEntitysFlat = imagesByFolder.values.flatten()
                val fromFolders = selectedFolders.flatMap { f -> allImageEntitysFlat.filter { it.path.startsWith(f.id) } }
                (selectedImageEntitys.toList() + fromFolders).distinctBy { it.uri }
                    .mapNotNull { runCatching { Uri.parse(it.uri) }.getOrNull() }
            }
        }
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete ImageEntity(s)") },
            text = { Text("Choose how you want to delete the selected image(s).") },
            confirmButton = {
                TextButton(
                    onClick = {
                        fileOpsViewModel.deleteImageEntitys(context, selectedUrisForDelete, trash = true)
                        selectedFolders = emptySet()
                        selectedImageEntitys = emptySet()
                        showDeleteConfirmation = false
                    }
                ) { Text("Move to Recycle Bin") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            fileOpsViewModel.deleteImageEntitys(context, selectedUrisForDelete, trash = false)
                            selectedFolders = emptySet()
                            selectedImageEntitys = emptySet()
                            showDeleteConfirmation = false
                        }
                    ) { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) }
                }
            }
        )
    }
}
