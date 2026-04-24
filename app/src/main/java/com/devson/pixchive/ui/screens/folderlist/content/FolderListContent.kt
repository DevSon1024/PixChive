package com.devson.pixchive.ui.screens.folderlist.content

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.devson.pixchive.data.ComicFolder
import com.devson.pixchive.data.local.ImageEntity
import com.devson.pixchive.ui.components.EmptyFoldersView
import com.devson.pixchive.ui.screens.folderlist.folder.FolderGridItem
import com.devson.pixchive.ui.screens.folderlist.folder.FolderListItem
import com.devson.pixchive.ui.screens.folderlist.model.LayoutMode
import com.devson.pixchive.ui.screens.folderlist.model.ViewSettings

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderListContent(
    folders: List<ComicFolder>,
    settings: ViewSettings,
    selectedFolders: Set<ComicFolder>,
    onFolderClick: (ComicFolder) -> Unit,
    onFolderLongClick: (ComicFolder) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val haptic = LocalHapticFeedback.current
    val sortedFolders = remember(folders) { folders.sortedBy { it.name.lowercase() } }

    if (sortedFolders.isEmpty()) {
        EmptyFoldersView()
        return
    }

    if (settings.layoutMode == LayoutMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(settings.gridColumns),
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
            items(sortedFolders) { folder ->
                FolderGridItem(
                    folder = folder,
                    settings = settings,
                    isSelected = folder in selectedFolders,
                    onClick = { onFolderClick(folder) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFolderLongClick(folder)
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
                bottom = contentPadding.calculateBottomPadding() + 32.dp
            )
        ) {
            items(sortedFolders) { folder ->
                FolderListItem(
                    folder = folder,
                    settings = settings,
                    isSelected = folder in selectedFolders,
                    onClick = { onFolderClick(folder) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFolderLongClick(folder)
                    }
                )
            }
        }
    }
}
