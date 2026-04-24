package com.devson.pixchive.ui.screens.folderlist

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
import com.devson.pixchive.data.ComicFolder
import com.devson.pixchive.data.local.ImageEntity
import com.devson.pixchive.ui.screens.folderlist.model.ViewMode
import com.devson.pixchive.ui.screens.folderlist.model.ViewSettings
import com.devson.pixchive.ui.components.CustomRenameDialog
import com.devson.pixchive.ui.components.PreviewFloatingActionButton
import com.devson.pixchive.ui.components.ViewSettingsBottomSheet
import com.devson.pixchive.ui.screens.folderlist.content.FolderListContent
import com.devson.pixchive.ui.screens.InformationBottomSheet
import com.devson.pixchive.ui.screens.StorageExplorerScreen
import com.devson.pixchive.ui.screens.folderlist.topbar.FolderListTopAppBar
import com.devson.pixchive.ui.screens.folderlist.content.ImageListContent
import com.devson.pixchive.ui.screens.folderlist.content.ExplorerListContent
import com.devson.pixchive.ui.screens.folderlist.selection.SelectionBottomBar
import com.devson.pixchive.ui.screens.folderlist.utils.applyFolderSort
import com.devson.pixchive.ui.screens.folderlist.utils.shareImages
import com.devson.pixchive.viewmodel.FileOperationsViewModel
import com.devson.pixchive.viewmodel.HomeViewModel
import com.devson.pixchive.viewmodel.FolderViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FolderListScreen(
    onImageSelected: (ImageEntity, List<ImageEntity>, Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onBack: () -> Unit = {},
    onNavigateToSearch: (String) -> Unit = {},
    viewModel: FolderViewModel = viewModel()
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
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
            // Permission granted, trigger any necessary load if not already handled by VM init
        }
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            permissionLauncher.launch(permission)
        }
    }

    val folders by viewModel.folders.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedFolder by viewModel.currentFolder.collectAsState()
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
    var selectedImages by remember { mutableStateOf(emptySet<ImageEntity>()) }
    var showInfoBottomSheet by remember { mutableStateOf(false) }

    val isSelectionActive = selectedFolders.isNotEmpty() || selectedImages.isNotEmpty()

    // Hoisted scroll states - survive recomposition and view-mode toggling
    val folderListState = rememberLazyListState()
    val folderGridState = rememberLazyGridState()
    var currentFolderId by rememberSaveable { mutableStateOf<String?>(null) }
    val imageListState = rememberLazyListState()
    val imageGridState = rememberLazyGridState()

    // Reset image scroll position when entering a different folder
    LaunchedEffect(selectedFolder) {
        if (selectedFolder?.id != currentFolderId) {
            imageListState.scrollToItem(0)
            imageGridState.scrollToItem(0)
            currentFolderId = selectedFolder?.id
        }
    }

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
                selectedImages = emptySet()
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
            // viewModel.refreshCurrentFolder() // Reload logic
            fileOpsViewModel.clearResult()
        }
    }

    val needsRefresh by fileOpsViewModel.needsRefresh.collectAsState()
    LaunchedEffect(needsRefresh) {
        if (needsRefresh) {
            viewModel.refreshCurrentFolder()
            fileOpsViewModel.onRefreshHandled()
        }
    }

    // Drives progress bar visibility
    val opInProgress by fileOpsViewModel.operationInProgress.collectAsState()

    // Back handler: clears selection first before navigating out
    BackHandler(enabled = selectedFolder != null || isSelectionActive || (viewSettings.viewMode == ViewMode.FOLDERS && currentExplorerPath != null)) {
        when {
            selectedImages.isNotEmpty() -> selectedImages = emptySet()
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
                ViewMode.ALL_FOLDERS -> selectedFolder?.name ?: "All Folders"
                ViewMode.FILES -> "All Files"
                ViewMode.FOLDERS -> currentExplorerPath?.substringAfterLast('/') ?: "Explorer"
            }
            FolderListTopAppBar(
                isSelectionActive = isSelectionActive,
                titleText = titleText,
                selectedCount = selectedImages.size + selectedFolders.size,
                totalCount = when (viewSettings.viewMode) {
                    ViewMode.ALL_FOLDERS -> if (selectedFolder != null) chapters.sumOf { it.images.size } else folders.size
                    ViewMode.FILES -> 0 // Placeholder for paging total count
                    ViewMode.FOLDERS -> explorerNodes.first.size + explorerNodes.second.size
                },
                showBackButton = selectedFolder != null || (viewSettings.viewMode == ViewMode.FOLDERS && currentExplorerPath != null),
                onClearSelection = { 
                    selectedFolders = emptySet()
                    selectedImages = emptySet()
                },
                onSelectAll = {
                    when (viewSettings.viewMode) {
                        ViewMode.ALL_FOLDERS -> {
                            if (selectedFolder != null) {
                                val allImages = chapters.flatMap { it.images }
                                selectedImages = if (selectedImages.size == allImages.size) emptySet() else allImages.toSet()
                            } else {
                                selectedFolders = if (selectedFolders.size == folders.size) emptySet() else folders.toSet()
                            }
                        }
                        ViewMode.FILES -> {
                            // Selection all in paging is complex, maybe handle later or only for visible?
                        }
                        ViewMode.FOLDERS -> {
                            val (expFolders, expImages) = explorerNodes
                            val allSelected = selectedFolders.size == expFolders.size && selectedImages.size == expImages.size
                            if (allSelected) {
                                selectedFolders = emptySet()
                                selectedImages = emptySet()
                            } else {
                                selectedFolders = expFolders.toSet()
                                selectedImages = expImages.toSet()
                            }
                        }
                    }
                },
                onBack = {
                    if (selectedFolder != null) {
                        viewModel.selectFolder(null)
                    } else if (viewSettings.viewMode == ViewMode.FOLDERS && currentExplorerPath != null) {
                        viewModel.navigateExplorerUp()
                    } else {
                        onBack()
                    }
                },
                onNavigateToSettings = onNavigateToSettings,
                onShowSettings = { showSettingsSheet = true },
                onBackToFolders = { viewModel.selectFolder(null) },
                searchActive = searchActive,
                searchText = searchText,
                onSearchActiveChange = { searchActive = it },
                onSearchTextChange = { 
                    searchText = it
                    viewModel.onSearchQueryChanged(it)
                },
                searchSuggestions = searchSuggestions,
                searchFocusRequester = searchFocusRequester,
                keyboard = keyboard
            )
        },
                            val allExpImages = explorerNodes.second
                            val allExpFolders = explorerNodes.first
                            if (selectedImages.size + selectedFolders.size == allExpImages.size + allExpFolders.size) {
                                selectedImages = emptySet()
                                selectedFolders = emptySet()
                            } else {
                                selectedImages = allExpImages.toSet()
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
                val allImagesFlat = remember(imagesByFolder) { imagesByFolder.values.flatten() }
                val selectedUris: List<Uri> = remember(
                    viewSettings.viewMode, selectedImages, selectedFolders, selectedFolder, imagesByFolder
                ) {
                    when (viewSettings.viewMode) {
                        ViewMode.FILES -> {
                            selectedImages.mapNotNull { runCatching { Uri.parse(it.uri) }.getOrNull() }
                        }
                        ViewMode.ALL_FOLDERS -> {
                            if (selectedFolder != null) {
                                // Inside a folder: operate on selected individual images
                                selectedImages.mapNotNull { runCatching { Uri.parse(it.uri) }.getOrNull() }
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
                                .flatMap { f -> allImagesFlat.filter { it.path.startsWith(f.id) } }
                            (selectedImages.toList() + fromFolders)
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
                        onImageSelected = { image, playlist ->
                            onImageSelected(image, playlist, historyMap[image.uri]?.lastPositionMs ?: 0L)
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
                            shareImages(context, images)
                            selectedFolders = emptySet()
                            selectedImages = emptySet()
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
                            selectedImages = emptySet()
                        }
                    )
                } else {
                    // FILES, FOLDERS mode, or ALL_FOLDERS inside a specific folder:
                    // use the image-centric bar driven by the unified selectedUris list
                    ImageSelectionBottomBar(
                        selectedImages = selectedImages,
                        onPlayAll = {
                            val playImage = selectedImages.firstOrNull()
                            if (playImage != null) {
                                val playlist = when (viewSettings.viewMode) {
                                    ViewMode.FILES -> imagesByFolder.values.flatten().applySort(viewSettings.sortField, viewSettings.sortDirection)
                                    ViewMode.ALL_FOLDERS -> (imagesByFolder[selectedFolder] ?: emptyList()).applySort(viewSettings.sortField, viewSettings.sortDirection)
                                    ViewMode.FOLDERS -> explorerNodes.second.applySort(viewSettings.sortField, viewSettings.sortDirection)
                                }
                                onImageSelected(playImage, playlist, historyMap[playImage.uri]?.lastPositionMs ?: 0L)
                                selectedImages = emptySet()
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
                            val image = selectedImages.firstOrNull()
                            if (image != null) {
                                renameInputText = image.title.substringBeforeLast(".")
                                showRenameDialog = true
                            }
                        },
                        onShare = {
                            shareImages(context, selectedImages.toList())
                            selectedImages = emptySet()
                            selectedFolders = emptySet()
                        },
                        onShowInfo = { showInfoBottomSheet = true },
                        onMarkStatus = { status ->
                            selectedImages.forEach { image ->
                                val position = when(status) {
                                    "NEW" -> 0L
                                    "RUNNING" -> image.duration / 2
                                    "ENDED" -> image.duration
                                    else -> 0L
                                }
                                homeViewModel.setWatchStatus(image, position)
                            }
                            selectedImages = emptySet()
                            selectedFolders = emptySet()
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (viewSettings.showFloatingButton && !isSelectionActive) {
                val allImagesFlat = remember(imagesByFolder) { imagesByFolder.values.flatten() }

                val lastPlayedImage = remember(history, selectedFolder, viewSettings.viewMode, currentExplorerPath, allImagesFlat) {
                    if (viewSettings.viewMode == ViewMode.ALL_FOLDERS && selectedFolder != null) {
                        val folderImages = imagesByFolder[selectedFolder] ?: emptyList()
                        val folderUris = folderImages.map { it.uri }.toSet()
                        val lastHistory = history.firstOrNull { it.uri in folderUris }
                        if (lastHistory != null) folderImages.find { it.uri == lastHistory.uri } else null
                    } else if (viewSettings.viewMode == ViewMode.FOLDERS && currentExplorerPath != null) {
                        val pathImages = allImagesFlat.filter { it.path.startsWith(currentExplorerPath!!) }
                        val pathUris = pathImages.map { it.uri }.toSet()
                        val lastHistory = history.firstOrNull { it.uri in pathUris }
                        if (lastHistory != null) pathImages.find { it.uri == lastHistory.uri } else null
                    } else {
                        val lastHistory = history.firstOrNull()
                        if (lastHistory != null) allImagesFlat.find { it.uri == lastHistory.uri } else null
                    }
                }

                if (lastPlayedImage != null) {
                    val lastHistoryEntry = remember(lastPlayedImage, historyMap) { historyMap[lastPlayedImage.uri] }
                    PreviewFloatingActionButton(
                        enablePreview = viewSettings.enableFabPreview,
                        previewUri = lastPlayedImage.uri,
                        previewTitle = lastPlayedImage.title,
                        previewDurationMs = lastPlayedImage.duration,
                        previewLastPositionMs = lastHistoryEntry?.lastPositionMs ?: 0L,
                        onPlay = {
                            val playlist = when (viewSettings.viewMode) {
                                ViewMode.FILES -> allImagesFlat.applySort(viewSettings.sortField, viewSettings.sortDirection)
                                ViewMode.ALL_FOLDERS -> if (selectedFolder != null) {
                                    (imagesByFolder[selectedFolder] ?: emptyList()).applySort(viewSettings.sortField, viewSettings.sortDirection)
                                } else {
                                    allImagesFlat.applySort(viewSettings.sortField, viewSettings.sortDirection)
                                }
                                ViewMode.FOLDERS -> if (currentExplorerPath != null) {
                                    allImagesFlat.filter { it.path.startsWith(currentExplorerPath!!) }.applySort(viewSettings.sortField, viewSettings.sortDirection)
                                } else {
                                    allImagesFlat.applySort(viewSettings.sortField, viewSettings.sortDirection)
                                }
                            }
                            onImageSelected(lastPlayedImage, playlist, lastHistoryEntry?.lastPositionMs ?: 0L)
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
                    onRefresh = { viewModel.loadImages(forceRefresh = true) },
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
                                val sortedImages = remember(images, viewSettings.sortField, viewSettings.sortDirection) {
                                    images.applySort(viewSettings.sortField, viewSettings.sortDirection)
                                }
                                ImageListContent(
                                    images = sortedImages,
                                    settings = viewSettings,
                                    selectedImages = selectedImages,
                                    onImageClick = { image ->
                                        if (isSelectionActive) {
                                            selectedImages = if (image in selectedImages) selectedImages - image else selectedImages + image
                                        } else {
                                            onImageSelected(image, sortedImages, historyMap[image.uri]?.lastPositionMs ?: 0L)
                                        }
                                    },
                                    onImageLongClick = { image ->
                                        selectedImages = if (image in selectedImages) selectedImages - image else selectedImages + image
                                    },
                                    listState = videoListState,
                                    gridState = videoGridState,
                                    historyMap = historyMap,
                                    contentPadding = padding
                                )
                            }
                        }
                        ViewMode.FILES -> {
                            val allImages = remember(imagesByFolder) { imagesByFolder.values.flatten() }
                            val sortedImages = remember(allImages, viewSettings.sortField, viewSettings.sortDirection) {
                                allImages.applySort(viewSettings.sortField, viewSettings.sortDirection)
                            }
                            ImageListContent(
                                images = sortedImages,
                                settings = viewSettings,
                                selectedImages = selectedImages,
                                onImageClick = { image ->
                                    if (isSelectionActive) {
                                        selectedImages = if (image in selectedImages) selectedImages - image else selectedImages + image
                                    } else {
                                        onImageSelected(image, sortedImages, historyMap[image.uri]?.lastPositionMs ?: 0L)
                                    }
                                },
                                onImageLongClick = { image ->
                                    selectedImages = if (image in selectedImages) selectedImages - image else selectedImages + image
                                },
                                listState = videoListState,
                                gridState = videoGridState,
                                historyMap = historyMap,
                                contentPadding = padding
                            )
                        }
                        ViewMode.FOLDERS -> {
                            val (expFolders, expImages) = explorerNodes
                            val sortedExpImages = remember(expImages, viewSettings.sortField, viewSettings.sortDirection) {
                                expImages.applySort(viewSettings.sortField, viewSettings.sortDirection)
                            }
                            // Need a map to resolve explorer nodes content
                            val mappedImagesByFolder = remember(imagesByFolder, expFolders) {
                                val allImages = imagesByFolder.values.flatten()
                                expFolders.associateWith { folder ->
                                    allImages.filter { it.path.startsWith(folder.id) }
                                }
                            }
                            val sortedExpFolders = remember(expFolders, viewSettings.sortField, viewSettings.sortDirection, mappedImagesByFolder) {
                                expFolders.applyFolderSort(mappedImagesByFolder, viewSettings.sortField, viewSettings.sortDirection)
                            }
                            val allImagesForSize = remember(imagesByFolder) { imagesByFolder.values.flatten() }

                            ExplorerListContent(
                                folders = sortedExpFolders,
                                images = sortedExpImages,
                                allImagesForSize = allImagesForSize,
                                settings = viewSettings,
                                selectedFolders = selectedFolders,
                                selectedImages = selectedImages,
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
                                onImageClick = { image ->
                                    if (isSelectionActive) {
                                        selectedImages = if (image in selectedImages) selectedImages - image else selectedImages + image
                                    } else {
                                        onImageSelected(image, sortedExpImages, historyMap[image.uri]?.lastPositionMs ?: 0L)
                                    }
                                },
                                onImageLongClick = { image ->
                                    selectedImages = if (image in selectedImages) selectedImages - image else selectedImages + image
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
            ViewMode.FILES -> selectedImages
            ViewMode.ALL_FOLDERS -> {
                if (selectedFolder != null) {
                    selectedImages
                } else {
                    selectedFolders.flatMap { imagesByFolder[it] ?: emptyList() }.toSet()
                }
            }
            ViewMode.FOLDERS -> {
                val allImagesFlat = imagesByFolder.values.flatten()
                val fromFolders = selectedFolders.flatMap { f -> 
                    allImagesFlat.filter { it.path.startsWith(f.id) } 
                }
                (selectedImages + fromFolders).toSet()
            }
        }
        InformationBottomSheet(
            selectedImages = imagesToShow,
            onDismiss = { showInfoBottomSheet = false }
        )
    }

    // ---- RENAME DIALOG ----
    if (showRenameDialog && (selectedFolders.size == 1 || selectedImages.size == 1)) {
        val isFolder = selectedFolders.size == 1 && selectedFolder == null
        val title = if (isFolder) "Rename Folder" else "Rename image"
        
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
                    val image = if (selectedFolder != null) selectedImages.firstOrNull() 
                               else (imagesByFolder[selectedFolders.first()] ?: emptyList()).firstOrNull()
                    if (image != null) {
                        fileOpsViewModel.renameImage(context, Uri.parse(image.uri), newName)
                    }
                }
                showRenameDialog = false
                selectedFolders = emptySet()
                selectedImages = emptySet()
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
            ViewMode.FILES -> selectedImages.mapNotNull { runCatching { Uri.parse(it.uri) }.getOrNull() }
            ViewMode.ALL_FOLDERS -> {
                if (selectedFolder != null) {
                    selectedImages.mapNotNull { runCatching { Uri.parse(it.uri) }.getOrNull() }
                } else {
                    selectedFolders.flatMap { folder -> imagesByFolder[folder] ?: emptyList() }
                        .mapNotNull { runCatching { Uri.parse(it.uri) }.getOrNull() }
                }
            }
            ViewMode.FOLDERS -> {
                val allImagesFlat = imagesByFolder.values.flatten()
                val fromFolders = selectedFolders.flatMap { f -> allImagesFlat.filter { it.path.startsWith(f.id) } }
                (selectedImages.toList() + fromFolders).distinctBy { it.uri }
                    .mapNotNull { runCatching { Uri.parse(it.uri) }.getOrNull() }
            }
        }
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete image(s)") },
            text = { Text("Choose how you want to delete the selected image(s).") },
            confirmButton = {
                TextButton(
                    onClick = {
                        fileOpsViewModel.deleteImages(context, selectedUrisForDelete, trash = true)
                        selectedFolders = emptySet()
                        selectedImages = emptySet()
                        showDeleteConfirmation = false
                    }
                ) { Text("Move to Recycle Bin") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            fileOpsViewModel.deleteImages(context, selectedUrisForDelete, trash = false)
                            selectedFolders = emptySet()
                            selectedImages = emptySet()
                            showDeleteConfirmation = false
                        }
                    ) { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) }
                }
            }
        )
    }
}
