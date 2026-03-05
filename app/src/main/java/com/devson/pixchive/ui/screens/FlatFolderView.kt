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
import kotlinx.coroutines.flow.filter

/**
 * Self-contained Flat view composable.
 *
 * Flow collection happens HERE, inside the composable, so Compose automatically
 * cancels the [FolderViewModel.flatImages] paging collection the moment this
 * composable leaves the composition (i.e. when the user switches to Explorer mode).
 *
 * This prevents the Pager from loading pages in the background while the Explorer
 * view is active, saving both RAM and CPU.
 */
@Composable
fun FlatFolderView(
    folderId: String,
    layoutMode: String,
    gridColumns: Int,
    initialScrollIndex: Int = 0,
    initialScrollOffset: Int = 0,
    onSaveScroll: (Int, Int) -> Unit = { _, _ -> },
    onImageClick: (Int) -> Unit,
    viewModel: FolderViewModel = viewModel()
) {
    // Collected here — cancelled automatically when FlatFolderView leaves composition.
    val images = viewModel.flatImages.collectAsLazyPagingItems()

    // Only show the empty state once the initial load has completed.
    // If we return early while loadState.refresh is still Loading, the
    // LazyGrid is never added to the composition, the Pager never receives
    // a collector, and no pages are ever fetched — causing a blank screen.
    val isRefreshing = images.loadState.refresh is LoadState.Loading
    if (!isRefreshing && images.itemCount == 0) { EmptyImagesView(); return }

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
                    ImageGridItem(image, gridColumns, { onImageClick(index) }, {
                        viewModel.refreshFolder(folderId)
                    })
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
                    ImageListItem(image, { onImageClick(index) }, {
                        viewModel.refreshFolder(folderId)
                    })
                }
            }
        }
    }
}
