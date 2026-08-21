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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.devson.pixchive.core.data.FileOperationsViewModel
import com.devson.pixchive.core.data.models.GalleryImage
import com.devson.pixchive.core.designsystem.component.PixChiveEmptyState
import com.devson.pixchive.core.designsystem.component.SkeletonLoadingView
import com.devson.pixchive.core.utils.FormatUtils
import com.devson.pixchive.feature.gallery.ui.components.CustomRenameDialog
import com.devson.pixchive.feature.gallery.ui.components.DetailsDialog
import com.devson.pixchive.feature.gallery.ui.components.GalleryImageItem
import com.devson.pixchive.feature.gallery.ui.components.GalleryImageListItem
import com.devson.pixchive.feature.gallery.ui.components.GallerySelectionBottomBar
import com.devson.pixchive.feature.gallery.ui.components.GalleryViewSettingsBottomSheet
import com.devson.pixchive.feature.gallery.ui.components.StandardAlbumHeroHeader
import com.devson.pixchive.feature.gallery.viewmodel.GalleryFolderViewModel

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
    val pagedImages: LazyPagingItems<GalleryImage> = viewModel.pagedImages.collectAsLazyPagingItems()
    val savedGridCellsIndex by viewModel.gridCellsIndex.collectAsState()
    val layoutMode by viewModel.layoutMode.collectAsState()
    val viewSettings by viewModel.viewSettings.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val folderName by viewModel.folderName.collectAsState()
    val albumMetadata by viewModel.albumMetadata.collectAsState()
    val galleryCoverAspectRatio by viewModel.galleryCoverAspectRatio.collectAsState()

    val selectedImageIds by viewModel.selectedIds.collectAsState()
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showStorageExplorer by remember { mutableStateOf(false) }
    var explorerOperationType by remember { mutableStateOf("") }

    val fileOpsViewModel: FileOperationsViewModel = viewModel(key = "fileops_folder")
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
    val listState = rememberLazyListState()

    val isScrolledPastHeader by remember(layoutMode) {
        derivedStateOf {
            if (layoutMode == "list") {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 240
            } else {
                gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 240
            }
        }
    }

    BackHandler(enabled = selectedImageIds.isNotEmpty()) {
        viewModel.clearSelection()
    }

    LaunchedEffect(bucketId) {
        viewModel.loadImages(bucketId)
    }

    // Collect current page items into a stable snapshot for selection & metadata helpers.
    val snapshotImages: List<GalleryImage> = remember(pagedImages.itemCount) {
        (0 until pagedImages.itemCount).mapNotNull { pagedImages.peek(it) }
    }

    val selectedImages = remember(selectedImageIds, snapshotImages) {
        snapshotImages.filter { it.id in selectedImageIds }
    }

    // Album Hero Header metadata values
    val displayedAlbumName = remember(albumMetadata.folderName, folderName) {
        albumMetadata.folderName.ifEmpty { folderName }
    }
    val displayedCoverUri = remember(albumMetadata.coverImageUri, snapshotImages) {
        albumMetadata.coverImageUri ?: snapshotImages.firstOrNull()?.uri
    }
    val displayedTotalImages = remember(albumMetadata.totalImages, pagedImages.itemCount) {
        if (albumMetadata.totalImages > 0) albumMetadata.totalImages else pagedImages.itemCount
    }
    val displayedTotalSize = remember(albumMetadata.totalSizeFormatted, snapshotImages) {
        if (albumMetadata.totalSizeFormatted.isNotBlank()) {
            albumMetadata.totalSizeFormatted
        } else {
            FormatUtils.formatFileSize(snapshotImages.sumOf { it.size })
        }
    }

    // Determine loading / empty states from pager.
    val isInitialLoad = pagedImages.loadState.refresh is LoadState.Loading
    val isEmpty = pagedImages.loadState.refresh is LoadState.NotLoading &&
            pagedImages.itemCount == 0

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                            IconButton(onClick = { viewModel.selectAll(snapshotImages) }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier.statusBarsPadding()
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                when {
                    isInitialLoad && pagedImages.itemCount == 0 -> {
                        val baseColumns = if (layoutMode == "list") 1 else (4 - savedGridCellsIndex.coerceIn(0, 2))
                        SkeletonLoadingView(
                            layoutMode = layoutMode,
                            columns = baseColumns,
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                        )
                    }
                    isEmpty -> {
                        PixChiveEmptyState(
                            icon = Icons.Default.PhotoLibrary,
                            title = "Folder is Empty",
                            message = "No images found in this folder."
                        )
                    }
                    else -> {
                        if (layoutMode == "list") {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(bottom = 100.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Full-width Hero Header as top item
                                item(key = "hero_header") {
                                    StandardAlbumHeroHeader(
                                        albumName = displayedAlbumName,
                                        coverImageUri = displayedCoverUri,
                                        totalImages = displayedTotalImages,
                                        albumSizeFormatted = displayedTotalSize,
                                        onNavigateBack = onNavigateBack,
                                        onActionClick = {
                                            if (pagedImages.itemCount > 0) onImageClick(0)
                                        },
                                        onOptionsClick = { showSettingsSheet = true },
                                        overlineText = "PHOTO ALBUM"
                                    )
                                }

                                item(key = "list_top_spacer") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                items(
                                    count = pagedImages.itemCount,
                                    key = pagedImages.itemKey { it.id }
                                ) { index ->
                                    val image = pagedImages[index] ?: return@items
                                    val isSelected = image.id in selectedImageIds
                                    Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                                        GalleryImageListItem(
                                            image = image,
                                            isSelected = isSelected,
                                            isSelectionModeActive = selectedImageIds.isNotEmpty(),
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

                                item(key = "list_bottom_spacer") {
                                    Spacer(modifier = Modifier.navigationBarsPadding().height(24.dp))
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
                                contentPadding = PaddingValues(bottom = 100.dp),
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
                                // Full-width Hero Header spanning all columns
                                item(key = "hero_header", span = { GridItemSpan(maxLineSpan) }) {
                                    StandardAlbumHeroHeader(
                                        albumName = displayedAlbumName,
                                        coverImageUri = displayedCoverUri,
                                        totalImages = displayedTotalImages,
                                        albumSizeFormatted = displayedTotalSize,
                                        onNavigateBack = onNavigateBack,
                                        onActionClick = {
                                            if (pagedImages.itemCount > 0) onImageClick(0)
                                        },
                                        onOptionsClick = { showSettingsSheet = true },
                                        overlineText = "PHOTO ALBUM"
                                    )
                                }

                                item(key = "grid_top_spacer", span = { GridItemSpan(maxLineSpan) }) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                items(
                                    count = pagedImages.itemCount,
                                    key = pagedImages.itemKey { it.id }
                                ) { index ->
                                    val image = pagedImages[index] ?: return@items
                                    val isSelected = image.id in selectedImageIds
                                    GalleryImageItem(
                                        image = image,
                                        isSelected = isSelected,
                                        isSelectionModeActive = selectedImageIds.isNotEmpty(),
                                        isListMode = false,
                                        columnCount = animatedColumns.coerceIn(2, 4),
                                        viewSettings = viewSettings,
                                        aspectRatio = galleryCoverAspectRatio,
                                        modifier = Modifier
                                            .animateItem()
                                            .fillMaxWidth(),
                                        onThumbnailClick = { viewModel.toggleSelection(image.id) },
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

                                item(key = "grid_bottom_spacer", span = { GridItemSpan(maxLineSpan) }) {
                                    Spacer(modifier = Modifier.navigationBarsPadding().height(24.dp))
                                }
                            }
                        }
                    }
                }

                // Smooth Glassmorphic Sticky Top Bar shown when user scrolls past the hero header
                AnimatedVisibility(
                    visible = isScrolledPastHeader && selectedImageIds.isEmpty(),
                    enter = fadeIn(tween(220)) + slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(220)
                    ),
                    exit = fadeOut(tween(180)) + slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(180)
                    ),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 3.dp,
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .height(56.dp)
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = displayedAlbumName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { showSettingsSheet = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "View Settings",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                thickness = 0.8.dp
                            )
                        }
                    }
                }

                // Floating Selection Bottom Bar
                AnimatedVisibility(
                    visible = selectedImageIds.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                ) {
                    GallerySelectionBottomBar(
                        selectedImages = selectedImages,
                        selectedCount = selectedImageIds.size,
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
                aspectRatio = galleryCoverAspectRatio,
                onAspectRatioChange = { viewModel.setGalleryCoverAspectRatio(it) },
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
                        viewModel.renameSelectedImage(newName, snapshotImages)
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