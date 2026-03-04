package com.devson.pixchive.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.compose.itemContentType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devson.pixchive.data.Chapter
import com.devson.pixchive.data.local.ImageEntity
import com.devson.pixchive.ui.components.DisplayOptionsSheet
import com.devson.pixchive.ui.components.ImageGridItem
import com.devson.pixchive.ui.components.ImageListItem
import com.devson.pixchive.ui.components.ChapterListItem
import com.devson.pixchive.ui.components.EmptyChaptersView
import com.devson.pixchive.ui.components.EmptyImagesView
import com.devson.pixchive.ui.components.SkeletonGrid
import com.devson.pixchive.ui.components.SkeletonList
import com.devson.pixchive.viewmodel.FolderViewModel
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderViewScreen(
    folderId: String,
    onNavigateBack: () -> Unit,
    onChapterClick: (String) -> Unit,
    onImageClick: (Int) -> Unit,
    viewModel: FolderViewModel = viewModel()
) {
    val currentFolder by viewModel.currentFolder.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val lazyImages = viewModel.flatImages.collectAsLazyPagingItems()
    val isLoading by viewModel.isLoading.collectAsState()

    val layoutMode by viewModel.layoutMode.collectAsState()
    val currentViewMode by viewModel.viewMode.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()

    // Scroll position restoration
    val flatScrollIndex  by viewModel.flatScrollIndex.collectAsState()
    val flatScrollOffset by viewModel.flatScrollOffset.collectAsState()
    val explorerScrollIndex  by viewModel.explorerScrollIndex.collectAsState()
    val explorerScrollOffset by viewModel.explorerScrollOffset.collectAsState()

    var showDisplayOptions by remember { mutableStateOf(false) }

    LaunchedEffect(folderId) { viewModel.loadFolder(folderId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentFolder?.displayName ?: "Loading...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { viewModel.refreshFolder(folderId) }) { Icon(Icons.Default.Refresh, "Refresh") }
                    IconButton(onClick = { showDisplayOptions = true }) { Icon(Icons.Default.Tune, "Options") }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                if (layoutMode == "grid") {
                    SkeletonGrid(columns = gridColumns)
                } else {
                    SkeletonList()
                }
            } else {
                when (currentViewMode) {
                    "explorer" -> ExplorerView(
                        chapters, layoutMode, gridColumns, isLoading,
                        initialScrollIndex = explorerScrollIndex,
                        initialScrollOffset = explorerScrollOffset,
                        onSaveScroll = { idx, off -> viewModel.saveExplorerScrollPosition(idx, off) },
                        onChapterClick = onChapterClick
                    ) { viewModel.removeFolder(it) }
                    "flat" -> FlatView(
                        lazyImages, layoutMode, gridColumns,
                        initialScrollIndex = flatScrollIndex,
                        initialScrollOffset = flatScrollOffset,
                        onSaveScroll = { idx, off -> viewModel.saveFlatScrollPosition(idx, off) },
                        onImageClick = onImageClick
                    ) { viewModel.refreshFolder(folderId) }
                    else -> ExplorerView(
                        chapters, layoutMode, gridColumns, isLoading,
                        initialScrollIndex = explorerScrollIndex,
                        initialScrollOffset = explorerScrollOffset,
                        onSaveScroll = { idx, off -> viewModel.saveExplorerScrollPosition(idx, off) },
                        onChapterClick = onChapterClick
                    ) { viewModel.removeFolder(it) }
                }
            }
        }

        if (showDisplayOptions) {
            DisplayOptionsSheet(
                onDismiss = { showDisplayOptions = false },
                viewMode = currentViewMode, layoutMode = layoutMode, gridColumns = gridColumns, sortOption = sortOption,
                onViewModeChange = { viewModel.setViewMode(it) },
                onLayoutModeChange = { viewModel.setLayoutMode(it) },
                onGridColumnsChange = { viewModel.setGridColumns(it) },
                onSortOptionChange = { viewModel.setSortOption(it) }
            )
        }
    }
}

@Composable
fun ExplorerView(
    chapters: List<Chapter>,
    layoutMode: String,
    gridColumns: Int,
    isLoading: Boolean,
    initialScrollIndex: Int = 0,
    initialScrollOffset: Int = 0,
    onSaveScroll: (Int, Int) -> Unit = { _, _ -> },
    onChapterClick: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    if (!isLoading && chapters.isEmpty()) { EmptyChaptersView(); return }

    if (layoutMode == "grid") {
        val gridState = rememberLazyGridState(
            initialFirstVisibleItemIndex = initialScrollIndex,
            initialFirstVisibleItemScrollOffset = initialScrollOffset
        )
        // Save only when scrolling fully stops — avoids per-pixel coroutine spam that causes ANR
        LaunchedEffect(gridState) {
            snapshotFlow { gridState.isScrollInProgress }
                .filter { !it }
                .collect { onSaveScroll(gridState.firstVisibleItemIndex, 0) }
        }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(gridColumns),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chapters, key = { it.path }) { chapter ->
                ChapterGridItem(chapter, gridColumns, { onChapterClick(chapter.path) }, { onRemove(chapter.path) })
            }
        }
    } else {
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = initialScrollIndex,
            initialFirstVisibleItemScrollOffset = initialScrollOffset
        )
        // Save only when scrolling fully stops — avoids per-pixel coroutine spam that causes ANR
        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress }
                .filter { !it }
                .collect { onSaveScroll(listState.firstVisibleItemIndex, 0) }
        }
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 16.dp)) {
            items(chapters, key = { it.path }) { chapter ->
                ChapterListItem(chapter, { onChapterClick(chapter.path) }, { onRemove(chapter.path) })
            }
        }
    }
}

@Composable
fun FlatView(
    images: LazyPagingItems<ImageEntity>,
    layoutMode: String,
    gridColumns: Int,
    initialScrollIndex: Int = 0,
    initialScrollOffset: Int = 0,
    onSaveScroll: (Int, Int) -> Unit = { _, _ -> },
    onImageClick: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    if (images.itemCount == 0) { EmptyImagesView(); return }

    if (layoutMode == "grid") {
        val gridState = rememberLazyGridState(
            initialFirstVisibleItemIndex = initialScrollIndex,
            initialFirstVisibleItemScrollOffset = initialScrollOffset
        )
        // Save only when scrolling fully stops — avoids per-pixel coroutine spam that causes ANR
        LaunchedEffect(gridState) {
            snapshotFlow { gridState.isScrollInProgress }
                .filter { !it }
                .collect { onSaveScroll(gridState.firstVisibleItemIndex, 0) }
        }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(gridColumns),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                count = images.itemCount,
                key = images.itemKey { it.path },
                contentType = images.itemContentType { "image" }
            ) { index ->
                val image = images[index]
                if (image != null) {
                    ImageGridItem(image, gridColumns, { onImageClick(index) }, onRefresh)
                }
            }
        }
    } else {
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = initialScrollIndex,
            initialFirstVisibleItemScrollOffset = initialScrollOffset
        )
        // Save only when scrolling fully stops — avoids per-pixel coroutine spam that causes ANR
        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress }
                .filter { !it }
                .collect { onSaveScroll(listState.firstVisibleItemIndex, 0) }
        }
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 16.dp)) {
            items(
                count = images.itemCount,
                key = images.itemKey { it.path },
                contentType = images.itemContentType { "image" }
            ) { index ->
                val image = images[index]
                if (image != null) {
                    ImageListItem(image, { onImageClick(index) }, onRefresh)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChapterGridItem(
    chapter: Chapter,
    columns: Int,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    val showDetails = columns <= 2
    val showName = columns <= 4
    val fetchSize = if (columns <= 2) 600 else 300

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .combinedClickable(onClick = onClick, onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); showMenu = true })
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (chapter.images.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(chapter.images.first().uri)
                            .size(fetchSize)
                            .crossfade(false)
                            .build(),
                        contentDescription = chapter.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Folder, null, modifier = Modifier.align(Alignment.Center).size(64.dp), tint = MaterialTheme.colorScheme.primary)
                }

                if (showName) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(chapter.displayName, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (showDetails) {
                                Text("${chapter.imageCount} images", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("Remove") }, onClick = { showMenu = false; onRemove() }, leadingIcon = { Icon(Icons.Default.Close, null) })
        }
    }
}