package com.devson.pixchive.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
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
import com.devson.pixchive.ui.components.ChapterListItem
import com.devson.pixchive.ui.components.EmptyChaptersView
import com.devson.pixchive.viewmodel.FolderViewModel
import kotlinx.coroutines.flow.filter
import androidx.compose.ui.graphics.Color

/**
 * Self-contained Explorer (chapter) view composable.
 *
 * [FolderViewModel.chapters] is collected HERE so Compose cancels the expensive group-and-sort
 * StateFlow collection the moment the user switches to Flat mode. Combined with
 * [SharingStarted.WhileSubscribed(5000)] in the ViewModel, the heavy flatMap + groupBy +
 * sort pipeline shuts down 5 s after it loses its last subscriber, saving significant CPU.
 */
@Composable
fun ExplorerFolderView(
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

    if (!isLoading && chapters.isEmpty()) { EmptyChaptersView(); return }

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
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(gridColumns),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
    } else {
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = initialScrollIndex,
            initialFirstVisibleItemScrollOffset = initialScrollOffset
        )
        // Save only when scrolling fully stops - avoids per-pixel coroutine spam that causes ANR
        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress }
                .filter { !it }
                .collect { onSaveScroll(listState.firstVisibleItemIndex, 0) }
        }
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 16.dp)) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChapterGridItem(
    chapter: Chapter,
    columns: Int,
    savedPage: Int = 0,
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
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                )
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
                    Icon(
                        Icons.Default.Folder,
                        null,
                        modifier = Modifier.align(Alignment.Center).size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (showName) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
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
                }

                // Progress bar overlay - shown when user has started reading this chapter
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
