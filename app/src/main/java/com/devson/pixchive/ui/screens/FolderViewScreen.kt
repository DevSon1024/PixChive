package com.devson.pixchive.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
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
import com.devson.pixchive.data.ImageFile
import com.devson.pixchive.ui.components.DisplayOptionsSheet
import com.devson.pixchive.ui.components.ImageGridItem
import com.devson.pixchive.ui.components.ImageListItem
import com.devson.pixchive.ui.components.ChapterListItem
import com.devson.pixchive.ui.components.EmptyChaptersView
import com.devson.pixchive.ui.components.EmptyImagesView
import com.devson.pixchive.ui.components.VerticalFastScroller
import com.devson.pixchive.viewmodel.FolderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderViewScreen(
    folderId: String,
    onNavigateBack: () -> Unit,
    onChapterClick: (String) -> Unit,
    onImageClick: (Int) -> Unit,
    viewModel: FolderViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentFolder by viewModel.currentFolder.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val allImages by viewModel.allImages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val layoutMode by viewModel.layoutMode.collectAsState()
    val currentViewMode by viewModel.viewMode.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()

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
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                when (currentViewMode) {
                    "explorer" -> ExplorerView(chapters, layoutMode, gridColumns, onChapterClick) { viewModel.removeFolder(it) }
                    "flat" -> FlatView(allImages, layoutMode, gridColumns, onImageClick) { viewModel.refreshFolder(folderId) }
                    else -> ExplorerView(chapters, layoutMode, gridColumns, onChapterClick) { viewModel.removeFolder(it) }
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
    onChapterClick: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    if (chapters.isEmpty()) { EmptyChaptersView(); return }

    if (layoutMode == "grid") {
        val gridState = rememberLazyGridState()
        VerticalFastScroller(listState = gridState) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chapters) { chapter ->
                    ChapterGridItem(chapter, gridColumns, { onChapterClick(chapter.path) }, { onRemove(chapter.path) })
                }
            }
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(chapters) { chapter ->
                ChapterListItem(chapter, { onChapterClick(chapter.path) }, { onRemove(chapter.path) })
            }
        }
    }
}

@Composable
fun FlatView(
    images: List<ImageFile>,
    layoutMode: String,
    gridColumns: Int,
    onImageClick: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    if (images.isEmpty()) { EmptyImagesView(); return }

    if (layoutMode == "grid") {
        val gridState = rememberLazyGridState()
        VerticalFastScroller(listState = gridState) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(images.size) { index ->
                    ImageGridItem(images[index], gridColumns, { onImageClick(index) }, onRefresh)
                }
            }
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(images.size) { index ->
                ImageListItem(images[index], { onImageClick(index) }, onRefresh)
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

    // Smart display logic
    val showDetails = columns <= 2
    val showName = columns <= 4

    // Approx size fetch
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