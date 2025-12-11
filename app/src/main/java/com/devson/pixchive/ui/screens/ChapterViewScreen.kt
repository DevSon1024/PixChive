package com.devson.pixchive.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.data.ImageFile
import com.devson.pixchive.ui.components.DisplayOptionsSheet
import com.devson.pixchive.ui.components.ImageGridItem
import com.devson.pixchive.ui.components.ChapterImageListItem
import com.devson.pixchive.ui.components.EmptyChapterImagesView
import com.devson.pixchive.ui.components.VerticalFastScroller
import com.devson.pixchive.viewmodel.FolderViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private fun urisMatch(uri1: String, uri2: String): Boolean {
    return try {
        val parsed1 = Uri.parse(uri1)
        val parsed2 = Uri.parse(uri2)
        parsed1.scheme == parsed2.scheme && parsed1.authority == parsed2.authority && parsed1.path == parsed2.path
    } catch (e: Exception) { uri1 == uri2 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterViewScreen(
    folderId: String,
    chapterPath: String,
    onNavigateBack: () -> Unit,
    onImageClick: (Int) -> Unit,
    viewModel: FolderViewModel = viewModel()
) {
    val layoutMode by viewModel.layoutMode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()

    var showDisplayOptions by remember { mutableStateOf(false) }

    val chapterImages = remember(chapters, chapterPath) {
        chapters.find { urisMatch(it.path, chapterPath) }?.images ?: emptyList()
    }
    val chapterName = remember(chapterPath) { chapterPath.substringAfterLast("/").substringAfterLast(":") }
    val onRefresh = { viewModel.refreshFolder(folderId) }

    LaunchedEffect(folderId) { viewModel.loadFolder(folderId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chapterName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { IconButton(onClick = { showDisplayOptions = true }) { Icon(Icons.Default.Tune, "Options") } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (chapterImages.isEmpty()) {
                EmptyChapterImagesView(chapterName, chapters.size)
            } else {
                if (layoutMode == "grid") {
                    val gridState = rememberLazyGridState()
                    VerticalFastScroller(listState = gridState) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(gridColumns),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(chapterImages) { index, image ->
                                ImageGridItem(image, gridColumns, { onImageClick(index) }, onRefresh)
                            }
                        }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                        itemsIndexed(chapterImages) { index, image ->
                            ChapterImageListItem(image, { onImageClick(index) }, onRefresh)
                        }
                    }
                }
            }
        }
        if (showDisplayOptions) {
            DisplayOptionsSheet(
                onDismiss = { showDisplayOptions = false }, viewMode = null, layoutMode = layoutMode, gridColumns = gridColumns, sortOption = null,
                onLayoutModeChange = { viewModel.setLayoutMode(it) }, onGridColumnsChange = { viewModel.setGridColumns(it) }
            )
        }
    }
}