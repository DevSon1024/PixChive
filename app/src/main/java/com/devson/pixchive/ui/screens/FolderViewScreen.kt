package com.devson.pixchive.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devson.pixchive.data.Chapter
import com.devson.pixchive.data.ImageFile
import com.devson.pixchive.viewmodel.FolderViewModel
import com.devson.pixchive.ui.components.ImageListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderViewScreen(
    folderId: String,
    onNavigateBack: () -> Unit,
    onChapterClick: (String) -> Unit,
    onImageClick: (Int) -> Unit,
    viewModel: FolderViewModel = viewModel()
) {
    val currentFolder by viewModel.currentFolder.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val allImages by viewModel.allImages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val layoutMode by viewModel.layoutMode.collectAsState()
    val currentViewMode by viewModel.viewMode.collectAsState()
    var showViewModeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(folderId) {
        viewModel.loadFolder(folderId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentFolder?.displayName ?: "Loading...",
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
                    IconButton(onClick = {
                        viewModel.refreshFolder(folderId)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                    // View Mode Selector
                    IconButton(onClick = { showViewModeDialog = true }) {
                        Icon(
                            imageVector = when (currentViewMode) {
                                "explorer" -> Icons.Default.FolderOpen
                                "flat" -> Icons.Default.Collections
                                else -> Icons.Default.FolderOpen
                            },
                            contentDescription = "Change View Mode"
                        )
                    }

                    // Layout Toggle
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
                else -> {
                    when (currentViewMode) {
                        "explorer" -> ExplorerView(
                            chapters = chapters,
                            layoutMode = layoutMode,
                            onChapterClick = onChapterClick
                        )
                        "flat" -> FlatView(
                            images = allImages,
                            layoutMode = layoutMode,
                            onImageClick = onImageClick
                        )
                        // Removed "chapter" view mode logic to reduce redundancy
                        "chapter" -> ExplorerView(
                            chapters = chapters,
                            layoutMode = layoutMode,
                            onChapterClick = onChapterClick
                        )
                    }
                }
            }
        }

        if (showViewModeDialog) {
            ViewModeDialog(
                currentMode = currentViewMode,
                onModeSelected = { mode ->
                    // This saves to prefs via ViewModel
                    viewModel.setViewMode(mode)
                    showViewModeDialog = false
                },
                onDismiss = { showViewModeDialog = false }
            )
        }
    }
}

@Composable
fun ExplorerView(
    chapters: List<Chapter>,
    layoutMode: String,
    onChapterClick: (String) -> Unit
) {
    if (chapters.isEmpty()) {
        EmptyChaptersView()
        return
    }

    if (layoutMode == "grid") {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chapters) { chapter ->
                ChapterGridItem(
                    chapter = chapter,
                    onClick = { onChapterClick(chapter.path) }
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chapters) { chapter ->
                ChapterListItem(
                    chapter = chapter,
                    onClick = { onChapterClick(chapter.path) }
                )
            }
        }
    }
}

@Composable
fun FlatView(
    images: List<ImageFile>,
    layoutMode: String,
    onImageClick: (Int) -> Unit
) {
    if (images.isEmpty()) {
        EmptyImagesView()
        return
    }

    if (layoutMode == "grid") {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(images.size) { index ->
                ImageGridItem(
                    image = images[index],
                    onClick = { onImageClick(index) }
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
        ) {
            items(images.size) { index ->
                ImageListItem(
                    image = images[index],
                    onClick = { onImageClick(index) },
                    onOptionClick = { /* Handle option click */ }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterGridItem(
    chapter: Chapter,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (chapter.images.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(chapter.images.first().uri)
                        .size(600) // Optimization: Small thumbnail for chapter cover
                        .crossfade(true)
                        .build(),
                    contentDescription = chapter.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = chapter.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${chapter.imageCount} images",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListItem(
    chapter: Chapter,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (chapter.images.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(chapter.images.first().uri)
                        .size(300) // Optimization: Small thumbnail
                        .crossfade(true)
                        .build(),
                    contentDescription = chapter.displayName,
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${chapter.imageCount} images",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ImageGridItem(
    image: ImageFile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable(onClick = onClick)
    ) {
        // OPTIMIZATION: Load small thumbnail for grid
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(image.uri)
                .size(600) // Critical for performance in Flat View
                .crossfade(true)
                .build(),
            contentDescription = image.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageListItem(
    image: ImageFile,
    index: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // OPTIMIZATION: Load small thumbnail for list
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image.uri)
                    .size(300)
                    .crossfade(true)
                    .build(),
                contentDescription = image.name,
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Image #$index",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = image.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ... ViewModeDialog and Empty views remain the same ...
@Composable
fun ViewModeDialog(
    currentMode: String,
    onModeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select View Mode") },
        text = {
            Column {
                ViewModeOption(
                    icon = Icons.Default.FolderOpen,
                    title = "Explorer View",
                    description = "Browse by chapters/folders",
                    isSelected = currentMode == "explorer",
                    onClick = { onModeSelected("explorer") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ViewModeOption(
                    icon = Icons.Default.Collections,
                    title = "Flat View",
                    description = "Show all images from all chapters",
                    isSelected = currentMode == "flat",
                    onClick = { onModeSelected("flat") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// ... Keep ViewModeOption, EmptyChaptersView, EmptyImagesView from original file ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewModeOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyChaptersView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "📂", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "No Chapters Found", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun EmptyImagesView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "🖼️", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "No Images Found", style = MaterialTheme.typography.titleLarge)
        }
    }
}