package com.devson.pixchive.gallery.ui.components.list

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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devson.pixchive.gallery.data.models.GalleryImage

@Composable
fun ImageListContent(
    images: List<GalleryImage>,
    selectedImages: Set<GalleryImage>,
    layoutMode: String = "grid",
    gridColumns: Int = 3,
    showThumbnail: Boolean = true,
    showFileExtension: Boolean = false,
    onImageClick: (GalleryImage) -> Unit,
    onImageLongClick: (GalleryImage) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    if (images.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No images found.",
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
                end = 8.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 8.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(images, key = { it.id }) { image ->
                ImageGridItem(
                    image = image,
                    isSelected = image in selectedImages,
                    gridColumns = gridColumns,
                    showThumbnail = showThumbnail,
                    showFileExtension = showFileExtension,
                    onClick = { onImageClick(image) },
                    onLongClick = { onImageLongClick(image) }
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 16.dp
            )
        ) {
            items(images, key = { it.id }) { image ->
                ImageListItem(
                    image = image,
                    isSelected = image in selectedImages,
                    showThumbnail = showThumbnail,
                    showFileExtension = showFileExtension,
                    onClick = { onImageClick(image) },
                    onLongClick = { onImageLongClick(image) }
                )
            }
        }
    }
}
