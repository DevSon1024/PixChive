package com.devson.pixchive.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
import com.devson.pixchive.ui.components.ChapterListItem
import com.devson.pixchive.ui.components.EmptyChaptersView
import com.devson.pixchive.viewmodel.FolderViewModel
import kotlinx.coroutines.flow.filter
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import java.io.File

@Composable
fun ExplorerFolderView(
    folderId: String,
    layoutMode: String,
    gridColumns: Int,
    isLoading: Boolean,
    readProgressMap: Map<String, Int> = emptyMap(),
    initialScrollIndex: Int = 0,
    initialScrollOffset: Int = 0,
    onSaveScroll: (Int, Int) -> Unit = { _, _ -> },
    onChapterClick: (String) -> Unit,
    viewModel: FolderViewModel = viewModel()
) {
    val chapters by viewModel.chapters.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val currentFolder by viewModel.currentFolder.collectAsState()
    val isStaleState = currentFolder?.id != folderId

    if (isStaleState) return

    if (!isLoading && chapters.isEmpty()) { EmptyChaptersView(); return }

    @OptIn(ExperimentalMaterial3Api::class)
    if (layoutMode == "grid") {
        val gridState = rememberLazyGridState(
            initialFirstVisibleItemIndex = initialScrollIndex,
            initialFirstVisibleItemScrollOffset = initialScrollOffset
        )
        // Save only when scrolling fully stops - avoids per-pixel coroutine spam that causes ANR
        LaunchedEffect(gridState) {
            snapshotFlow { gridState.isScrollInProgress }
                .filter { !it }
                .collect { onSaveScroll(gridState.firstVisibleItemIndex, 0) }
        }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshCurrentFolder() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // The keys are correct here
                items(chapters, key = { it.path }) { chapter ->
                    ChapterGridItem(
                        chapter = chapter,
                        columns = gridColumns,
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
            LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 16.dp), modifier = Modifier.fillMaxSize()) {
                // The keys are correct here
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChapterGridItem(
    chapter: Chapter,
    columns: Int,
    savedPage: Int = 0,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    val showDetails = columns <= 2
    val showName = columns <= 4
    // Reduced fetch size to prevent GC thrashing when rendering folder grids
    val fetchSize = if (columns <= 2) 400 else 250

    // CRITICAL FIX: Remember the request, use hardware bitmaps, and load from File path
    val firstImagePath = chapter.images.firstOrNull()?.path
    val imageRequest = remember(firstImagePath, fetchSize) {
        if (firstImagePath != null) {
            ImageRequest.Builder(context)
                .data(File(firstImagePath))
                .size(fetchSize)
                .allowHardware(true) // Stops the NativeAlloc GC stutter
                .crossfade(false)
                .build()
        } else null
    }

    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .graphicsLayer {
                    clip = true
                    shape = RoundedCornerShape(8.dp)
                }
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                )
        ) {
            if (firstImagePath != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = chapter.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Folder,
                    null,
                    modifier = Modifier.align(Alignment.Center).size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (showName) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .padding(8.dp)
                ) {
                    Text(
                        chapter.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (showDetails) {
                        Text(
                            "${chapter.imageCount} images",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (savedPage > 0 && chapter.imageCount > 0) {
                LinearProgressIndicator(
                    progress = { savedPage.toFloat() / chapter.imageCount },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Remove") },
                onClick = { showMenu = false; onRemove() },
                leadingIcon = { Icon(Icons.Default.Close, null) }
            )
        }
    }
}