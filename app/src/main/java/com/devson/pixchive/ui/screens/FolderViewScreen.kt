package com.devson.pixchive.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devson.pixchive.data.Chapter
import com.devson.pixchive.data.ImageFile
import com.devson.pixchive.ui.components.DisplayOptionsSheet
import com.devson.pixchive.viewmodel.FolderViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderViewScreen(
    folderId: String,
    onNavigateBack: () -> Unit,
    onChapterClick: (String) -> Unit,
    onImageClick: (Int) -> Unit,
    viewModel: FolderViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentFolder by viewModel.currentFolder.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val allImages by viewModel.allImages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // States
    val layoutMode by viewModel.layoutMode.collectAsState()
    val currentViewMode by viewModel.viewMode.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()

    var showDisplayOptions by remember { mutableStateOf(false) }

    LaunchedEffect(folderId) {
        viewModel.loadFolder(folderId)
    }

    val onRefresh = { viewModel.refreshFolder(folderId) }
    val onRemoveFolder = { path: String ->
        viewModel.removeFolder(path)
        Toast.makeText(context, "Folder removed from list", Toast.LENGTH_SHORT).show()
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
                    IconButton(onClick = { viewModel.refreshFolder(folderId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    // Display Options
                    IconButton(onClick = { showDisplayOptions = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Display Options")
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
                            gridColumns = gridColumns,
                            onChapterClick = onChapterClick,
                            onRemove = onRemoveFolder
                        )
                        "flat" -> FlatView(
                            images = allImages,
                            layoutMode = layoutMode,
                            gridColumns = gridColumns,
                            onImageClick = onImageClick,
                            onRefresh = onRefresh
                        )
                        else -> ExplorerView(
                            chapters = chapters,
                            layoutMode = layoutMode,
                            gridColumns = gridColumns,
                            onChapterClick = onChapterClick,
                            onRemove = onRemoveFolder
                        )
                    }
                }
            }
        }

        if (showDisplayOptions) {
            DisplayOptionsSheet(
                onDismiss = { showDisplayOptions = false },
                viewMode = currentViewMode,
                layoutMode = layoutMode,
                gridColumns = gridColumns,
                sortOption = sortOption,
                onViewModeChange = { viewModel.setViewMode(it) },
                onLayoutModeChange = { viewModel.setLayoutMode(it) },
                onGridColumnsChange = { viewModel.setGridColumns(it) },
                onSortOptionChange = { viewModel.setSortOption(it) }
            )
        }
    }
}

@Composable
fun ExplorerView(
    chapters: List<Chapter>,
    layoutMode: String,
    gridColumns: Int,
    onChapterClick: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    if (chapters.isEmpty()) {
        EmptyChaptersView()
        return
    }

    if (layoutMode == "grid") {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns), // Dynamic columns
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chapters) { chapter ->
                ChapterGridItem(
                    chapter = chapter,
                    onClick = { onChapterClick(chapter.path) },
                    onRemove = { onRemove(chapter.path) }
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(chapters) { chapter ->
                ChapterListItem(
                    chapter = chapter,
                    onClick = { onChapterClick(chapter.path) },
                    onRemove = { onRemove(chapter.path) }
                )
            }
        }
    }
}

@Composable
fun FlatView(
    images: List<ImageFile>,
    layoutMode: String,
    gridColumns: Int,
    onImageClick: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    if (images.isEmpty()) {
        EmptyImagesView()
        return
    }

    if (layoutMode == "grid") {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns), // Dynamic columns
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(images.size) { index ->
                ImageGridItem(
                    image = images[index],
                    onClick = { onImageClick(index) },
                    onRefresh = onRefresh
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(images.size) { index ->
                ImageListItem(
                    image = images[index],
                    onClick = { onImageClick(index) },
                    onRefresh = onRefresh
                )
            }
        }
    }
}

// ... Keep existing ChapterGridItem, ChapterListItem, ImageGridItem, ImageListItem, EmptyViews components ...

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChapterGridItem(
    chapter: Chapter,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (chapter.images.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(chapter.images.first().uri)
                            .size(600)
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

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Remove from list") },
                leadingIcon = { Icon(Icons.Default.Close, null) },
                onClick = {
                    showMenu = false
                    onRemove()
                }
            )
        }
    }
}

@Composable
fun ChapterListItem(
    chapter: Chapter,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.size(48.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (chapter.images.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(chapter.images.first().uri)
                            .size(200)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${chapter.imageCount} images",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Remove from list") },
                        leadingIcon = { Icon(Icons.Default.Close, null) },
                        onClick = {
                            showMenu = false
                            onRemove()
                        }
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 80.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageGridItem(
    image: ImageFile,
    onClick: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image.uri)
                    .size(600)
                    .crossfade(true)
                    .build(),
                contentDescription = image.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        ItemContextMenu(
            expanded = showMenu,
            onDismiss = { showMenu = false },
            onShare = { shareItem(context, image.uri) },
            onDelete = {
                if (deleteItem(context, File(image.path))) {
                    onRefresh()
                }
            }
        )
    }
}

@Composable
fun ImageListItem(
    image: ImageFile,
    onClick: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.size(48.dp),
                colors = CardDefaults.cardColors(containerColor = Color.LightGray)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(image.uri)
                        .size(200)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = image.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatFileSize(image.size)} | ${formatDate(image.dateModified)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ItemContextMenu(
                    expanded = showMenu,
                    onDismiss = { showMenu = false },
                    onShare = { shareItem(context, image.uri) },
                    onDelete = {
                        if (deleteItem(context, File(image.path))) {
                            onRefresh()
                        }
                    }
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 80.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun ItemContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Share") },
            leadingIcon = { Icon(Icons.Default.Share, null) },
            onClick = {
                onDismiss()
                onShare()
            }
        )
        DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            onClick = {
                onDismiss()
                onDelete()
            }
        )
    }
}

// Helper functions (shareItem, deleteItem, etc.) need to be here as well,
// similar to the previous version of FolderViewScreen.
private fun shareItem(context: Context, uri: android.net.Uri) {
    try {
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Image"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun deleteItem(context: Context, file: File): Boolean {
    return try {
        val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
        if (deleted) {
            Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()
            true
        } else {
            Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show()
            false
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        false
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown Date"
    val sdf = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
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