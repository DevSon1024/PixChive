package com.devson.pixchive.ui.screens.imagelist.components.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devson.pixchive.model.LayoutMode
import com.devson.pixchive.model.Image
import com.devson.pixchive.model.ViewSettings
import com.devson.pixchive.ui.components.CustomEmptyStateView

@Composable
fun ImageListContent(
    images: List<Image>,
    settings: ViewSettings,
    selectedImages: Set<Image>,
    onImageClick: (Image) -> Unit,
    onImageLongClick: (Image) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    if (images.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CustomEmptyStateView(
                heading  = "No Images Here",
                subtext  = "This folder appears to be empty. Try pulling down to refresh.",
                ctaLabel = "Scan Device for Images"
            )
        }
        return
    }
    if (settings.layoutMode == LayoutMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(settings.gridColumns),
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
            items(
                count = images.size,
                key = { images[it].uri.toString() }
            ) { index ->
                val image = images[index]
                ImageGridItem(
                    image = image,
                    settings = settings,
                    isSelected = image in selectedImages,
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
            items(
                count = images.size,
                key = { images[it].uri.toString() }
            ) { index ->
                val image = images[index]
                ImageListItem(
                    image = image,
                    settings = settings,
                    isSelected = image in selectedImages,
                    onClick = { onImageClick(image) },
                    onLongClick = { onImageLongClick(image) }
                )
            }
        }
    }
}
