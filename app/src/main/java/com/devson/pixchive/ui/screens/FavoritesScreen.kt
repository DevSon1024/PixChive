package com.devson.pixchive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.compose.itemContentType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.pixchive.data.local.ImageEntity
import com.devson.pixchive.ui.components.DisplayOptionsSheet
import com.devson.pixchive.ui.components.EmptyFavoritesView
import com.devson.pixchive.ui.components.ImageGridItem
import com.devson.pixchive.ui.components.ImageListItem
// import com.devson.pixchive.ui.components.VerticalFastScroller <-- REMOVED
import com.devson.pixchive.viewmodel.FolderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateBack: () -> Unit,
    onImageClick: (Int) -> Unit,
    viewModel: FolderViewModel
) {
    val folderId = "favorites"

    val lazyImages = viewModel.favoriteImages.collectAsLazyPagingItems()
    val isLoading by viewModel.isLoading.collectAsState()

    val layoutMode by viewModel.layoutMode.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val sortOption by viewModel.favoritesSortOption.collectAsState()

    var showDisplayOptions by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDisplayOptions = true }) {
                        Icon(Icons.Default.Tune, "Display Options")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading && lazyImages.itemCount == 0 -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                lazyImages.itemCount == 0 -> {
                    EmptyFavoritesView()
                }
                else -> {
                    if (layoutMode == "grid") {
                        FavoritesGridView(
                            images = lazyImages,
                            columns = gridColumns,
                            onImageClick = onImageClick,
                            onRefresh = {} // No manual refresh needed for favorites
                        )
                    } else {
                        FavoritesListView(
                            images = lazyImages,
                            onImageClick = onImageClick,
                            onRefresh = {}
                        )
                    }
                }
            }
        }

        if (showDisplayOptions) {
            DisplayOptionsSheet(
                onDismiss = { showDisplayOptions = false },
                viewMode = null,
                layoutMode = layoutMode,
                gridColumns = gridColumns,
                sortOption = sortOption,
                onLayoutModeChange = { viewModel.setLayoutMode(it) },
                onGridColumnsChange = { viewModel.setGridColumns(it) },
                onSortOptionChange = { viewModel.setFavoritesSortOption(it) }
            )
        }
    }
}

@Composable
fun FavoritesGridView(
    images: LazyPagingItems<ImageEntity>,
    columns: Int,
    onImageClick: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns),
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
                ImageGridItem(
                    image = image,
                    columns = columns,
                    onClick = { onImageClick(index) },
                    onRefresh = onRefresh
                )
            }
        }
    }
}

@Composable
fun FavoritesListView(
    images: LazyPagingItems<ImageEntity>,
    onImageClick: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
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
                    onRefresh = onRefresh
                )
            }
        }
    }
}