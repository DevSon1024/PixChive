package com.devson.pixchive.feature.reader.ui

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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.core.data.FileOperationsViewModel
import com.devson.pixchive.core.data.local.ImageEntity
import com.devson.pixchive.core.designsystem.component.EmptyChapterImagesView
import com.devson.pixchive.core.designsystem.component.SkeletonLoadingView
import com.devson.pixchive.core.designsystem.component.ViewSettingsBottomSheet
import com.devson.pixchive.core.utils.FormatUtils
import com.devson.pixchive.feature.gallery.ui.components.ImageGridItem
import com.devson.pixchive.feature.reader.ui.components.ChapterImageListItem
import com.devson.pixchive.feature.reader.ui.components.FolderHeroHeader
import com.devson.pixchive.feature.reader.ui.components.ImageDetailsDialog
import com.devson.pixchive.feature.reader.utils.urisMatch
import com.devson.pixchive.feature.reader.viewmodel.FolderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterViewScreen(
    folderId: String,
    chapterPath: String,
    onNavigateBack: () -> Unit,
    onImageClick: (Int) -> Unit,
    viewModel: FolderViewModel = viewModel(),
    fileOpsViewModel: FileOperationsViewModel = viewModel()
) {
    val context = LocalContext.current
    val layoutMode by viewModel.layoutMode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val folderMetadata by viewModel.folderMetadata.collectAsState()
    val readProgressMap by viewModel.readProgressMap.collectAsState()

    var showDisplayOptions by remember { mutableStateOf(false) }
    var selectedImageForDetails by remember { mutableStateOf<ImageEntity?>(null) }

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    val isScrolledPastHeader by remember(layoutMode) {
        derivedStateOf {
            if (layoutMode == "grid") {
                gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 240
            } else {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 240
            }
        }
    }

    val chapterImages = remember(chapters, chapterPath) {
        chapters.find { urisMatch(it.path, chapterPath) }?.images?.filterIsInstance<ImageEntity>() ?: emptyList()
    }
    val chapterName = remember(chapterPath) {
        chapterPath.substringAfterLast("/").substringAfterLast(":")
    }
    val chapterSizeFormatted = remember(chapterImages) {
        FormatUtils.formatFileSize(chapterImages.sumOf { it.size })
    }
    val coverUri = remember(chapterImages, folderMetadata) {
        chapterImages.firstOrNull()?.uri ?: folderMetadata.coverImageUri
    }
    val currentSavedPage = remember(readProgressMap, chapterPath) {
        readProgressMap[chapterPath] ?: 0
    }
    val lastReadProgress = remember(chapterImages, currentSavedPage) {
        if (chapterImages.isNotEmpty() && currentSavedPage > 0) {
            ((currentSavedPage + 1f) / chapterImages.size.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    val onRefresh = { viewModel.refreshFolder(folderId) }

    LaunchedEffect(folderId) {
        viewModel.loadFolder(folderId)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading && chapterImages.isEmpty()) {
                SkeletonLoadingView(
                    layoutMode = layoutMode,
                    columns = gridColumns,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                )
            } else if (chapterImages.isEmpty()) {
                EmptyChapterImagesView(chapterName, chapters.size)
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refreshCurrentFolder() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (layoutMode == "grid") {
                        var localColumns by remember(gridColumns) { mutableStateOf(gridColumns) }
                        var accumulatedZoom by remember { mutableFloatStateOf(1f) }

                        val animatedColumns by animateIntAsState(
                            targetValue = localColumns,
                            animationSpec = tween(300),
                            label = "columns_anim"
                        )

                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(animatedColumns.coerceIn(1, 4)),
                            contentPadding = PaddingValues(bottom = 24.dp),
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
                                                        val newCols = (localColumns - 1).coerceIn(1, 4)
                                                        if (newCols != localColumns) {
                                                            localColumns = newCols
                                                            viewModel.setGridColumns(newCols)
                                                        }
                                                        hasChangedInThisGesture = true
                                                    } else if (accumulatedZoom < 0.75f) {
                                                        val newCols = (localColumns + 1).coerceIn(1, 4)
                                                        if (newCols != localColumns) {
                                                            localColumns = newCols
                                                            viewModel.setGridColumns(newCols)
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
                            // Hero Header at the top spanning all columns
                            item(key = "hero_header", span = { GridItemSpan(maxLineSpan) }) {
                                FolderHeroHeader(
                                    folderName = chapterName.ifEmpty { folderMetadata.folderName },
                                    coverImageUri = coverUri,
                                    totalImages = chapterImages.size,
                                    folderSizeFormatted = chapterSizeFormatted,
                                    lastReadProgress = lastReadProgress,
                                    lastReadPage = currentSavedPage,
                                    onNavigateBack = onNavigateBack,
                                    onReadClick = {
                                        val targetPage = currentSavedPage.coerceIn(0, (chapterImages.size - 1).coerceAtLeast(0))
                                        onImageClick(targetPage)
                                    },
                                    onOptionsClick = { showDisplayOptions = true },
                                    overlineText = "CHAPTER ARCHIVE"
                                )
                            }

                            item(key = "grid_top_spacer", span = { GridItemSpan(maxLineSpan) }) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            itemsIndexed(chapterImages, key = { _, img -> img.path.ifEmpty { img.uri } }) { index, image ->
                                Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                                    ImageGridItem(
                                        image = image,
                                        columns = animatedColumns.coerceIn(1, 4),
                                        onClick = { onImageClick(index) },
                                        onShareClick = {
                                            fileOpsViewModel.sharePhysicalFile(context, image.path)
                                        },
                                        onDeleteClick = {
                                            fileOpsViewModel.deletePhysicalFile(context, image.path) {
                                                onRefresh()
                                            }
                                        }
                                    )
                                }
                            }

                            item(key = "grid_bottom_spacer", span = { GridItemSpan(maxLineSpan) }) {
                                Spacer(modifier = Modifier.navigationBarsPadding().height(24.dp))
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Hero Header as the first item so it scrolls up naturally
                            item(key = "hero_header") {
                                FolderHeroHeader(
                                    folderName = chapterName.ifEmpty { folderMetadata.folderName },
                                    coverImageUri = coverUri,
                                    totalImages = chapterImages.size,
                                    folderSizeFormatted = chapterSizeFormatted,
                                    lastReadProgress = lastReadProgress,
                                    lastReadPage = currentSavedPage,
                                    onNavigateBack = onNavigateBack,
                                    onReadClick = {
                                        val targetPage = currentSavedPage.coerceIn(0, (chapterImages.size - 1).coerceAtLeast(0))
                                        onImageClick(targetPage)
                                    },
                                    onOptionsClick = { showDisplayOptions = true },
                                    overlineText = "CHAPTER ARCHIVE"
                                )
                            }

                            item(key = "list_top_spacer") {
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            itemsIndexed(chapterImages, key = { _, img -> img.path.ifEmpty { img.uri } }) { index, image ->
                                ChapterImageListItem(
                                    image = image,
                                    onClick = { onImageClick(index) },
                                    onShareClick = {
                                        fileOpsViewModel.sharePhysicalFile(context, image.path)
                                    },
                                    onDeleteClick = {
                                        fileOpsViewModel.deletePhysicalFile(context, image.path) {
                                            onRefresh()
                                        }
                                    },
                                    onInfoClick = {
                                        selectedImageForDetails = image
                                    }
                                )
                            }

                            item(key = "list_bottom_spacer") {
                                Spacer(modifier = Modifier.navigationBarsPadding().height(24.dp))
                            }
                        }
                    }
                }
            }

            // Glassmorphic Sticky Top Bar that reveals as user scrolls up
            AnimatedVisibility(
                visible = isScrolledPastHeader,
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
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
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
                                text = chapterName.ifEmpty { folderMetadata.folderName },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { showDisplayOptions = true }) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Options",
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
        }

        if (showDisplayOptions) {
            ViewSettingsBottomSheet(
                onDismiss = { showDisplayOptions = false },
                viewMode = null,
                layoutMode = layoutMode,
                gridColumns = gridColumns,
                sortOption = null,
                onLayoutModeChange = { viewModel.setLayoutMode(it) },
                onGridColumnsChange = { viewModel.setGridColumns(it) }
            )
        }

        selectedImageForDetails?.let { entity ->
            ImageDetailsDialog(
                entity = entity,
                onDismiss = { selectedImageForDetails = null }
            )
        }
    }
}