package com.devson.pixchive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.pixchive.data.ImageFile
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

    val allImages by viewModel.allImages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val layoutMode by viewModel.layoutMode.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()

    var showDisplayOptions by remember { mutableStateOf(false) }

    // Initial load only. Updates are handled by Flow in VM.
    LaunchedEffect(Unit) {
        viewModel.loadFolder(folderId)
    }

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
                isLoading && allImages.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                allImages.isEmpty() -> {
                    EmptyFavoritesView()
                }
                else -> {
                    if (layoutMode == "grid") {
                        FavoritesGridView(
                            images = allImages,
                            columns = gridColumns,
                            onImageClick = onImageClick,
                            onRefresh = {} // No manual refresh needed for favorites
                        )
                    } else {
                        FavoritesListView(
                            images = allImages,
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
                onSortOptionChange = { viewModel.setSortOption(it) }
            )
        }
    }
}

@Composable
fun FavoritesGridView(
    images: List<ImageFile>,
    columns: Int,
    onImageClick: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    val gridState = rememberLazyGridState()

    // REMOVED VerticalFastScroller wrapper
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(images.size) { index ->
            ImageGridItem(
                image = images[index],
                columns = columns,
                onClick = { onImageClick(index) },
                onRefresh = onRefresh
            )
        }
    }
}

@Composable
fun FavoritesListView(
    images: List<ImageFile>,
    onImageClick: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(images.size) { index ->
            ImageListItem(
                image = images[index],
                onClick = { onImageClick(index) },
                onRefresh = onRefresh
            )
        }
    }
}