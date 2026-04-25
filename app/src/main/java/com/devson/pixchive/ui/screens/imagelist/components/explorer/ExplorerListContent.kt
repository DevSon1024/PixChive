package com.devson.pixchive.ui.screens.imagelist.components.explorer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.devson.pixchive.model.LayoutMode
import com.devson.pixchive.model.Image
import com.devson.pixchive.model.ImageFolder
import com.devson.pixchive.model.ViewSettings
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import com.devson.pixchive.ui.screens.imagelist.components.list.ImageGridItem
import com.devson.pixchive.ui.screens.imagelist.components.list.ImageListItem
import com.devson.pixchive.ui.screens.imagelist.components.folder.FolderGridItem
import com.devson.pixchive.ui.screens.imagelist.components.folder.FolderListItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExplorerListContent(
    folders: List<ImageFolder>,
    images: List<Image>,
    allImagesForSize: List<Image>,
    settings: ViewSettings,
    selectedFolders: Set<ImageFolder>,
    selectedImages: Set<Image>,
    onFolderClick: (ImageFolder) -> Unit,
    onFolderLongClick: (ImageFolder) -> Unit,
    onImageClick: (Image) -> Unit,
    onImageLongClick: (Image) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val haptic = LocalHapticFeedback.current

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
            items(folders) { folder ->
                val folderImages = remember(folder, allImagesForSize) { allImagesForSize.filter { it.path.startsWith(folder.id) } }
                FolderGridItem(
                    folder = folder,
                    images = folderImages,
                    settings = settings,
                    isSelected = folder in selectedFolders,
                    onClick = { onFolderClick(folder) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFolderLongClick(folder)
                    }
                )
            }
            items(images) { image ->
                ImageGridItem(
                    image = image,
                    settings = settings,
                    isSelected = image in selectedImages,
                    onClick = { onImageClick(image) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onImageLongClick(image)
                    }
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
            items(folders) { folder ->
                val folderImages = remember(folder, allImagesForSize) { allImagesForSize.filter { it.path.startsWith(folder.id) } }
                FolderListItem(
                    folder = folder,
                    images = folderImages,
                    settings = settings,
                    isSelected = folder in selectedFolders,
                    onClick = { onFolderClick(folder) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFolderLongClick(folder)
                    }
                )
            }
            items(images) { image ->
                ImageListItem(
                    image = image,
                    settings = settings,
                    isSelected = image in selectedImages,
                    onClick = { onImageClick(image) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onImageLongClick(image)
                    }
                )
            }
        }
    }
}
