package com.devson.pixchive.gallery.ui

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.gallery.data.models.GalleryImage
import com.devson.pixchive.gallery.ui.components.GalleryImageItem
import com.devson.pixchive.gallery.viewmodel.SearchViewModel
import com.devson.pixchive.gallery.viewmodel.AllImagesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    query: String,
    onBack: () -> Unit,
    onImageClick: (Int, List<GalleryImage>) -> Unit,
    searchViewModel: SearchViewModel = viewModel(),
    allImagesViewModel: AllImagesViewModel = viewModel()
) {
    val results by searchViewModel.searchResults.collectAsState()
    val isSearching by searchViewModel.isSearching.collectAsState()
    val gridCellsIndex by allImagesViewModel.gridCellsIndex.collectAsState()
    val viewSettings by allImagesViewModel.viewSettings.collectAsState()

    LaunchedEffect(query) {
        searchViewModel.performSearch(query)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Results for '$query'", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (results.isEmpty()) {
                Text(
                    text = "No images found for '$query'",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                var currentColumns by remember(gridCellsIndex) { mutableStateOf(4 - gridCellsIndex.coerceIn(0, 2)) }
                var accumulatedZoom by remember { mutableFloatStateOf(1f) }
                val animatedColumns by animateIntAsState(
                    targetValue = currentColumns,
                    animationSpec = tween(300),
                    label = "columns_anim"
                )
                val gridState = rememberLazyGridState()

                LazyVerticalGrid(
                    columns = GridCells.Fixed(animatedColumns.coerceIn(2, 4)),
                    state = gridState,
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
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
                                                val newCols = (currentColumns - 1).coerceIn(2, 4)
                                                if (newCols != currentColumns) {
                                                    currentColumns = newCols
                                                    allImagesViewModel.setGridCellsIndex(4 - newCols)
                                                }
                                                hasChangedInThisGesture = true
                                            } else if (accumulatedZoom < 0.75f) {
                                                val newCols = (currentColumns + 1).coerceIn(2, 4)
                                                if (newCols != currentColumns) {
                                                    currentColumns = newCols
                                                    allImagesViewModel.setGridCellsIndex(4 - newCols)
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
                    items(results, key = { it.id }) { image ->
                        GalleryImageItem(
                            image = image,
                            isSelected = false,
                            isListMode = false,
                            columnCount = animatedColumns.coerceIn(2, 4),
                            viewSettings = viewSettings,
                            onClick = {
                                val idx = results.indexOf(image)
                                onImageClick(idx, results)
                            },
                            onLongClick = {},
                            modifier = Modifier
                                .animateItem()
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}
