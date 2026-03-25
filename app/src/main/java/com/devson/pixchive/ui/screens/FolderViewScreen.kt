package com.devson.pixchive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.ui.components.DisplayOptionsSheet
import com.devson.pixchive.ui.components.SkeletonGrid
import com.devson.pixchive.ui.components.SkeletonList
import com.devson.pixchive.viewmodel.FolderViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

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
    val explorerScrollIndex   by viewModel.explorerScrollIndex.collectAsState()
    val explorerScrollOffset  by viewModel.explorerScrollOffset.collectAsState()

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
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val isStaleState = currentFolder?.id != folderId

            if (isLoading || isStaleState) {
                // Show skeleton while loadFolder is running
                if (layoutMode == "grid") SkeletonGrid(columns = gridColumns) else SkeletonList()
            } else {
                // ── KEY DESIGN POINT 
                // Each branch renders ONE composable. When the user switches modes,
                // Compose removes the old branch and adds the new one. The leaving
                // composable's internal collectAsState / collectAsLazyPagingItems call
                // is automatically cancelled - no manual cleanup needed.
                // ──────
                when (effectiveViewMode) {
                    "flat" -> FlatFolderView(
                        folderId = folderId,
                        layoutMode = layoutMode,
                        gridColumns = gridColumns,
                        initialScrollIndex = flatScrollIndex,
                        initialScrollOffset = flatScrollOffset,
                        onSaveScroll = { idx, off -> viewModel.saveFlatScrollPosition(idx, off) },
                        onImageClick = onImageClick,
                        viewModel = viewModel
                    )
                    else -> ExplorerFolderView(
                        folderId = folderId,
                        layoutMode = layoutMode,
                        gridColumns = gridColumns,
                        isLoading = isLoading,
                        readProgressMap = readProgressMap,
                        initialScrollIndex = explorerScrollIndex,
                        initialScrollOffset = explorerScrollOffset,
                        onSaveScroll = { idx, off -> viewModel.saveExplorerScrollPosition(idx, off) },
                        onChapterClick = onChapterClick,
                        viewModel = viewModel
                    )
                }
            }
        }

        if (showDisplayOptions) {
            DisplayOptionsSheet(
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