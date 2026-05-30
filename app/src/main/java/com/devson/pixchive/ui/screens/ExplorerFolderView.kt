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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween

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

    if (!isLoading && chapters.isEmpty()) { EmptyChaptersView(); return }

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

    val shape = RoundedCornerShape(12.dp)

    Box(modifier = Modifier.padding(2.dp)) {
        OutlinedCard(
            shape = shape,
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (showName) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.5f),
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                chapter.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (showDetails) {
                                Text(
                                    "${chapter.imageCount} images",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                if (savedPage > 0 && chapter.imageCount > 0) {
                    val progressPercent = ((savedPage.toFloat() / chapter.imageCount) * 100).toInt().coerceIn(0, 100)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = "$progressPercent%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
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