package com.devson.pixchive.gallery.ui.components.folder

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devson.pixchive.gallery.data.models.GalleryFolder

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderListContent(
    folders: List<GalleryFolder>,
    selectedFolders: Set<GalleryFolder>,
    layoutMode: String = "grid",
    gridColumns: Int = 3,
    showThumbnail: Boolean = true,
    onFolderClick: (GalleryFolder) -> Unit,
    onFolderLongClick: (GalleryFolder) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val sortedFolders = remember(folders) {
        folders.sortedBy { it.folderName.lowercase() }
    }

    if (sortedFolders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No image folders found.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    if (layoutMode == "grid") {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 8.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                end = 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 40.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sortedFolders, key = { it.bucketId }) { folder ->
                FolderGridItem(
                    folder = folder,
                    isSelected = folder in selectedFolders,
                    gridColumns = gridColumns,
                    showThumbnail = showThumbnail,
                    onClick = { onFolderClick(folder) },
                    onLongClick = { onFolderLongClick(folder) }
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 32.dp
            )
        ) {
            items(sortedFolders, key = { it.bucketId }) { folder ->
                FolderListItem(
                    folder = folder,
                    isSelected = folder in selectedFolders,
                    showThumbnail = showThumbnail,
                    onClick = { onFolderClick(folder) },
                    onLongClick = { onFolderLongClick(folder) }
                )
            }
        }
    }
}
