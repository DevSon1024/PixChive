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
import com.devson.pixchive.model.Image
import com.devson.pixchive.model.ImageFolder
import com.devson.pixchive.model.LayoutMode
import com.devson.pixchive.model.ViewMode
import com.devson.pixchive.model.applySort
import com.devson.pixchive.ui.components.CustomRenameDialog
import com.devson.pixchive.ui.components.ViewSettingsBottomSheet
import com.devson.pixchive.ui.screens.imagelist.components.folder.FolderListContent
import com.devson.pixchive.ui.screens.InformationBottomSheet
import com.devson.pixchive.ui.screens.StorageExplorerScreen
import com.devson.pixchive.ui.screens.imagelist.components.topbar.ImageListTopAppBar
import com.devson.pixchive.ui.screens.imagelist.components.list.ImageListContent
import com.devson.pixchive.ui.screens.imagelist.components.explorer.ExplorerListContent
import com.devson.pixchive.ui.screens.imagelist.components.selection.ImageSelectionBottomBar
import com.devson.pixchive.ui.screens.imagelist.utils.applyFolderSort
import com.devson.pixchive.ui.screens.imagelist.utils.shareImages
import com.devson.pixchive.utils.SelectionBottomAppBar
import com.devson.pixchive.viewmodel.FileOperationsViewModel
import com.devson.pixchive.viewmodel.HomeViewModel
import com.devson.pixchive.viewmodel.ImageListViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ImageListScreen(
    onImageSelected: (folderId: String, imageIndex: Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onBack: () -> Unit = {},
    onNavigateToSearch: (String) -> Unit = {},
    viewModel: ImageListViewModel = viewModel()
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
            viewModel.loadImages()
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
        } else {
            viewModel.loadImages()
        }
    }

    val imagesByFolderRaw by viewModel.imagesByFolder.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedFolderRaw by viewModel.selectedFolder.collectAsState()
    val viewSettings by viewModel.viewSettings.collectAsState()
    val currentExplorerPath by viewModel.currentExplorerPath.collectAsState()
    val searchSuggestionsRaw by viewModel.searchSuggestions.collectAsState()

    val imagesByFolder: Map<ImageFolder, List<Image>> = remember(imagesByFolderRaw) {
        imagesByFolderRaw.entries.associate { entry ->
            val folder = ImageFolder(id = entry.key, name = entry.key.substringAfterLast('/'))
            val images = entry.value.map {
                Image(
                    uri = "file://${it.path}",
                    title = it.name,
                    path = it.path,
                    folderId = entry.key,
                    folderName = entry.key.substringAfterLast('/')
                )
            }
            folder to images
        }
    }

    val selectedFolder: ImageFolder? = remember(selectedFolderRaw, imagesByFolder) {
        selectedFolderRaw?.let { raw ->
            imagesByFolder.keys.find { it.id == raw }
                ?: ImageFolder(id = raw, name = raw.substringAfterLast('/'))
        }
    }

    val searchSuggestions: List<Image> = remember(searchSuggestionsRaw) {
        searchSuggestionsRaw.map { 
            Image(
                uri = "file://${it.path}",
                title = it.name,
                path = it.path,
                folderId = it.path.substringBeforeLast("/"),
                folderName = it.path.substringBeforeLast("/").substringAfterLast('/')
            )
        }
    }

    val explorerNodes = remember { Pair(emptyList<ImageFolder>(), emptyList<Image>()) }
    var searchActive by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(searchActive) {
        if (searchActive) searchFocusRequester.requestFocus()
    }

    var showSettingsSheet by remember { mutableStateOf(false) }

    //  SELECTION STATE 
    var selectedFolders by remember { mutableStateOf(emptySet<ImageFolder>()) }
    var selectedImages by remember { mutableStateOf(emptySet<Image>()) }
    var showInfoBottomSheet by remember { mutableStateOf(false) }

    val sortedFolderKeys = remember(imagesByFolder, viewSettings.sortField, viewSettings.sortDirection) {
        val keys = imagesByFolder.keys.toList()
        keys.applyFolderSort(imagesByFolder, viewSettings.sortField, viewSettings.sortDirection)
    }
    val isSelectionActive = selectedFolders.isNotEmpty() || selectedImages.isNotEmpty()

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

    // Removed HomeViewModel and Watch History

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
            viewModel.loadImages()
            fileOpsViewModel.clearResult()
        }
    }

    val needsRefresh by fileOpsViewModel.needsRefresh.collectAsState()
    LaunchedEffect(needsRefresh) {
        if (needsRefresh) {
            viewModel.loadImages(forceRefresh = true)
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
                ViewMode.ALL_FOLDERS -> selectedFolder?.name
                ViewMode.FILES -> "All Files"
                ViewMode.FOLDERS -> currentExplorerPath?.substringBeforeLast('/')?.substringAfterLast('/') ?: "Folders"
            }
            ImageListTopAppBar(
                isSelectionActive = isSelectionActive,
                titleText = titleText,
                selectedCount = selectedImages.size + selectedFolders.size,
                totalCount = when (viewSettings.viewMode) {
                    ViewMode.ALL_FOLDERS -> if (selectedFolder != null) (imagesByFolder[selectedFolder] ?: emptyList()).size else sortedFolderKeys.size
                    ViewMode.FILES -> imagesByFolder.values.flatten().size
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
                                val allImages = imagesByFolder[selectedFolder] ?: emptyList()
                                selectedImages = if (selectedImages.size == allImages.size) emptySet() else allImages.toSet()
                            } else {
                                selectedFolders = if (selectedFolders.size == sortedFolderKeys.size) emptySet() else sortedFolderKeys.toSet()
                            }
                        }
                        ViewMode.FILES -> {
                            val allImages = imagesByFolder.values.flatten()
                            selectedImages = if (selectedImages.size == allImages.size) emptySet() else allImages.toSet()
                        }
                        ViewMode.FOLDERS -> {
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
                        onImageSelected = { image, images -> 
                            onImageSelected(image.folderId, images.indexOf(image).takeIf { it >= 0 } ?: 0)
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
                        onShowInfo = { showInfoBottomSheet = true }
                    )
                } else {
                    // FILES, FOLDERS mode, or ALL_FOLDERS inside a specific folder:
                    // use the image-centric bar driven by the unified selectedUris list
                    ImageSelectionBottomBar(
                        selectedImages = selectedImages,
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
                    )
                }
            }
        },
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
                            Manifest.permission.READ_MEDIA_IMAGES
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
                                    onFolderClick = { folder ->
                                        if (isSelectionActive) {
                                            selectedFolders =
                                                if (folder in selectedFolders) selectedFolders - folder else selectedFolders + folder
                                        } else {
                                            viewModel.selectFolder(folder.id)
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
                                            onImageSelected(selectedFolder?.id ?: "", sortedImages.indexOf(image))
                                        }
                                    },
                                    onImageLongClick = { image ->
                                        selectedImages = if (image in selectedImages) selectedImages - image else selectedImages + image
                                    },
                                    listState = imageListState,
                                    gridState = imageGridState,
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
                                        val folderId = image.path.substringBeforeLast("/")
                                        onImageSelected(folderId, sortedImages.indexOf(image))
                                    }
                                },
                                onImageLongClick = { image ->
                                    selectedImages = if (image in selectedImages) selectedImages - image else selectedImages + image
                                },
                                listState = imageListState,
                                gridState = imageGridState,
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
                                        val folderId = image.path.substringBeforeLast("/")
                                        onImageSelected(folderId, sortedExpImages.indexOf(image))
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
        val title = if (isFolder) "Rename Folder" else "Rename Image"
        
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
            layoutMode = if (viewSettings.layoutMode == LayoutMode.GRID) "grid" else "list",
            gridColumns = viewSettings.gridColumns,
            onLayoutModeChange = { modeStr ->
                val newMode = if (modeStr == "grid") LayoutMode.GRID else LayoutMode.LIST
                viewModel.updateViewSettings(viewSettings.copy(layoutMode = newMode))
            },
            onGridColumnsChange = { cols ->
                viewModel.updateViewSettings(viewSettings.copy(gridColumns = cols))
            },
            onDismiss = { showSettingsSheet = false }
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
            title = { Text("Delete Image(s)") },
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
