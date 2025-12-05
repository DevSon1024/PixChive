package com.devson.pixchive.ui.screens

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
// FIX: Added missing imports below
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devson.pixchive.data.ImageFile
import com.devson.pixchive.viewmodel.FolderViewModel

// Helper function to properly compare URIs regardless of encoding
private fun urisMatch(uri1: String, uri2: String): Boolean {
    return try {
        val parsed1 = Uri.parse(uri1)
        val parsed2 = Uri.parse(uri2)

        parsed1.scheme == parsed2.scheme &&
                parsed1.authority == parsed2.authority &&
                parsed1.path == parsed2.path
    } catch (e: Exception) {
        uri1 == uri2
    }
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

    val chapterImages = remember(chapters, chapterPath) {
        val found = chapters.find { chapter ->
            urisMatch(chapter.path, chapterPath)
        }
        found?.images ?: emptyList()
    }

    val chapterName = remember(chapterPath) {
        chapterPath.substringAfterLast("/").substringAfterLast(":")
    }

    LaunchedEffect(folderId) {
        viewModel.loadFolder(folderId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = chapterName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleLayoutMode() }) {
                        Icon(
                            imageVector = if (layoutMode == "grid") {
                                Icons.AutoMirrored.Filled.ViewList
                            } else {
                                Icons.Default.GridView
                            },
                            contentDescription = "Toggle Layout"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                chapterImages.isEmpty() -> {
                    EmptyChapterImagesView(chapterName, chapters.size)
                }
                else -> {
                    if (layoutMode == "grid") {
                        ImageGridView(
                            images = chapterImages,
                            onImageClick = onImageClick
                        )
                    } else {
                        ImageListView(
                            images = chapterImages,
                            onImageClick = onImageClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChapterImagesView(
    chapterName: String,
    totalChapters: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🖼️",
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Images Found",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Chapter: $chapterName",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Total chapters loaded: $totalChapters",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ImageGridView(
    images: List<ImageFile>,
    onImageClick: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(images) { index, image ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .clickable { onImageClick(index) }
            ) {
                // FIX: Use ImageRequest to downsample the image
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(image.uri)
                        .size(600) // Downsample to prevent OOM
                        .crossfade(true)
                        .build(),
                    contentDescription = image.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageListView(
    images: List<ImageFile>,
    onImageClick: (Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(images) { index, image ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onImageClick(index) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // FIX: Use ImageRequest to downsample here as well
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(image.uri)
                            .size(600)
                            .crossfade(true)
                            .build(),
                        contentDescription = image.name,
                        modifier = Modifier.size(100.dp),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Image ${index + 1} of ${images.size}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = image.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}