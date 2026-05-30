package com.devson.pixchive.ui.screens

import ChapterGridItem
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.ui.components.ChapterListItem
import com.devson.pixchive.ui.components.EmptyChaptersView
import com.devson.pixchive.ui.components.ViewSettingsBottomSheet
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
    val currentFolder   by viewModel.currentFolder.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val layoutMode      by viewModel.layoutMode.collectAsState()
    val currentViewMode by viewModel.viewMode.collectAsState()
    val hasSubfolders   by viewModel.hasSubfolders.collectAsState()
    val effectiveViewMode = if (hasSubfolders) currentViewMode else "flat"
    val gridColumns     by viewModel.gridColumns.collectAsState()
    val sortOption      by viewModel.sortOption.collectAsState()
    val readProgressMap by viewModel.readProgressMap.collectAsState()

    // Scroll position restoration (plain Int state - no heavy flow work)
    val flatScrollIndex       by viewModel.flatScrollIndex.collectAsState()
    val flatScrollOffset      by viewModel.flatScrollOffset.collectAsState()
    val allFoldersScrollIndex   by viewModel.allFoldersScrollIndex.collectAsState()
    val allFoldersScrollOffset  by viewModel.allFoldersScrollOffset.collectAsState()

    var showDisplayOptions by remember { mutableStateOf(false) }

    LaunchedEffect(folderId) { viewModel.loadFolder(folderId) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Silently checks for new/deleted files every time user returns to the app.
                // Because we fixed FolderScanner, this will not cause any UI stutter!
                viewModel.refreshFolder(folderId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        currentFolder?.displayName ?: "Loading...",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshFolder(folderId) }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    IconButton(onClick = { showDisplayOptions = true }) {
                        Icon(Icons.Default.Tune, "Options")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            val isStaleState = currentFolder?.id != folderId || 
                   (currentFolder != null && isLoading)

            if (isLoading || isStaleState) {
                Box(modifier = Modifier.padding(top = padding.calculateTopPadding())) {
                    if (layoutMode == "grid") SkeletonGrid(columns = gridColumns) else SkeletonList()
                }
            } else {
                when (effectiveViewMode) {
                    "flat" -> AllImagesView(
                        folderId = folderId,
                        layoutMode = layoutMode,
                        gridColumns = gridColumns,
                        initialScrollIndex = flatScrollIndex,
                        initialScrollOffset = flatScrollOffset,
                        onSaveScroll = { idx, off -> viewModel.saveFlatScrollPosition(idx, off) },
                        onImageClick = onImageClick,
                        viewModel = viewModel,
                        paddingValues = padding
                    )
                    else -> AllFoldersView(
                        folderId = folderId,
                        layoutMode = layoutMode,
                        gridColumns = gridColumns,
                        isLoading = isLoading,
                        readProgressMap = readProgressMap,
                        initialScrollIndex = allFoldersScrollIndex,
                        initialScrollOffset = allFoldersScrollOffset,
                        onSaveScroll = { idx, off -> viewModel.saveAllFoldersScrollPosition(idx, off) },
                        onChapterClick = onChapterClick,
                        viewModel = viewModel,
                        paddingValues = padding
                    )
                }
            }
        }

        if (showDisplayOptions) {
            ViewSettingsBottomSheet(
                onDismiss = { showDisplayOptions = false },
                viewMode = if (hasSubfolders) currentViewMode else null,
                layoutMode = layoutMode,
                gridColumns = gridColumns,
                sortOption = sortOption,
                onViewModeChange = { viewModel.setViewMode(it) },
                onLayoutModeChange = { viewModel.setLayoutMode(it) },
                onGridColumnsChange = { viewModel.setGridColumns(it) },
                onSortOptionChange = { viewModel.setSortOption(it) }
            )
        }
    }
}

@Composable
fun AllFoldersView(
    folderId: String,
    layoutMode: String,
    gridColumns: Int,
    isLoading: Boolean,
    readProgressMap: Map<String, Int> = emptyMap(),
    initialScrollIndex: Int = 0,
    initialScrollOffset: Int = 0,
    onSaveScroll: (Int, Int) -> Unit = { _, _ -> },
    onChapterClick: (String) -> Unit,
    viewModel: FolderViewModel = viewModel(),
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val chapters by viewModel.chapters.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val currentFolder by viewModel.currentFolder.collectAsState()
    val isStaleState = currentFolder?.id != folderId

    if (isStaleState) return

    if (!isLoading && chapters.isEmpty()) {
        EmptyChaptersView(); return
    }

    @OptIn(ExperimentalMaterial3Api::class)
    if (layoutMode == "grid") {
        val gridState = rememberLazyGridState(
            initialFirstVisibleItemIndex = initialScrollIndex,
            initialFirstVisibleItemScrollOffset = initialScrollOffset
        )
        LaunchedEffect(gridState) {
            snapshotFlow { gridState.isScrollInProgress }
                .filter { !it }
                .collect { onSaveScroll(gridState.firstVisibleItemIndex, 0) }
        }
        var localColumns by remember(gridColumns) { mutableStateOf(gridColumns) }
        var accumulatedZoom by remember { mutableFloatStateOf(1f) }

        val animatedColumns by animateIntAsState(
            targetValue = localColumns,
            animationSpec = tween(300),
            label = "columns_anim"
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshCurrentFolder() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(animatedColumns.coerceIn(1, 6)),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding() + 8.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp,
                    start = 8.dp,
                    end = 8.dp
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
                                            val newCols = (localColumns - 1).coerceIn(1, 6)
                                            if (newCols != localColumns) {
                                                localColumns = newCols
                                                viewModel.setGridColumns(newCols)
                                            }
                                            hasChangedInThisGesture = true
                                        } else if (accumulatedZoom < 0.75f) {
                                            val newCols = (localColumns + 1).coerceIn(1, 6)
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
                items(chapters, key = { it.path }) { chapter ->
                    ChapterGridItem(
                        chapter = chapter,
                        columns = animatedColumns.coerceIn(1, 6),
                        savedPage = readProgressMap[chapter.path] ?: 0,
                        onClick = { onChapterClick(chapter.path) },
                        onRemove = { viewModel.removeFolder(chapter.path) }
                    )
                }
            }
        }
    } else {
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = initialScrollIndex,
            initialFirstVisibleItemScrollOffset = initialScrollOffset
        )
        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress }
                .filter { !it }
                .collect { onSaveScroll(listState.firstVisibleItemIndex, 0) }
        }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshCurrentFolder() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                items(chapters, key = { it.path }) { chapter ->
                    ChapterListItem(
                        chapter = chapter,
                        onClick = { onChapterClick(chapter.path) },
                        onRemove = { viewModel.removeFolder(chapter.path) }
                    )
                }
            }
        }
    }
}