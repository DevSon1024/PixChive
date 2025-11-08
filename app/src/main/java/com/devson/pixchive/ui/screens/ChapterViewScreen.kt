package com.devson.pixchive.ui.screens

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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.devson.pixchive.data.ImageFile
import com.devson.pixchive.viewmodel.FolderViewModel

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

    // Get chapter images
    val chapterImages = remember(chapterPath) {
        viewModel.getChapterImages(chapterPath)
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
                    EmptyImagesView()
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
                AsyncImage(
                    model = image.uri,
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
                    AsyncImage(
                        model = image.uri,
                        contentDescription = image.name,
                        modifier = Modifier.size(100.dp),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Image ${index + 1}",
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