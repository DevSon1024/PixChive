package com.devson.pixchive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.paging.LoadState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.devson.pixchive.ui.components.EmptyImagesView
import com.devson.pixchive.ui.components.ImageGridItem
import com.devson.pixchive.ui.components.ImageListItem
import com.devson.pixchive.viewmodel.FolderViewModel
import com.devson.pixchive.viewmodel.FileOperationsViewModel
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.filter
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween

@Composable
fun AllImagesView(
    folderId: String,
    layoutMode: String,
    gridColumns: Int,
    initialScrollIndex: Int = 0,
    initialScrollOffset: Int = 0,
    onSaveScroll: (Int, Int) -> Unit = { _, _ -> },
    onImageClick: (Int) -> Unit,
    viewModel: FolderViewModel = viewModel(),
    fileOpsViewModel: FileOperationsViewModel = viewModel(),
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val currentFolder by viewModel.currentFolder.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isStaleState = currentFolder?.id != folderId

    if (isStaleState) return

    val images = viewModel.flatImages.collectAsLazyPagingItems()

    val isPagingRefreshing = images.loadState.refresh is LoadState.Loading
    if (!isPagingRefreshing && images.itemCount == 0) { EmptyImagesView(); return }

    @OptIn(ExperimentalMaterial3Api::class)
    key(folderId) {
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
                    columns = GridCells.Fixed(animatedColumns.coerceIn(1, 4)),
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
                    items(
                        count = images.itemCount,
                        key = images.itemKey { it.path },
                        contentType = images.itemContentType { "image" }
                    ) { index ->
                        val image = images[index]
                        if (image != null) {
                            ImageGridItem(
                                image = image,
                                columns = animatedColumns.coerceIn(1, 4),
                                onClick = { onImageClick(index) },
                                onShareClick = {
                                    fileOpsViewModel.sharePhysicalFile(context, image.path)
                                },
                                onDeleteClick = {
                                    fileOpsViewModel.deletePhysicalFile(context, image.path) {
                                        viewModel.refreshFolder(folderId)
                                    }
                                }
                            )
                        }
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
                        top = paddingValues.calculateTopPadding() + 4.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp,
                        start = 8.dp,
                        end = 8.dp
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        count = images.itemCount,
                        key = images.itemKey { it.path },
                        contentType = images.itemContentType { "image" }
                    ) { index ->
                        val image = images[index]
                        if (image != null) {
                            ImageListItem(
                                image = image,
                                onClick = { onImageClick(index) },
                                onShareClick = {
                                    fileOpsViewModel.sharePhysicalFile(context, image.path)
                                },
                                onDeleteClick = {
                                    fileOpsViewModel.deletePhysicalFile(context, image.path) {
                                        viewModel.refreshFolder(folderId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
