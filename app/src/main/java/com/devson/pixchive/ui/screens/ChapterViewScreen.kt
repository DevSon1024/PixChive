package com.devson.pixchive.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devson.pixchive.data.ImageFile
import com.devson.pixchive.ui.components.DisplayOptionsSheet
import com.devson.pixchive.ui.components.ImageGridItem
import com.devson.pixchive.viewmodel.FolderViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    // State from ViewModel
    val layoutMode by viewModel.layoutMode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()

    // Local UI State
    var showDisplayOptions by remember { mutableStateOf(false) }

    val chapterImages = remember(chapters, chapterPath) {
        val found = chapters.find { chapter ->
            urisMatch(chapter.path, chapterPath)
        }
        found?.images ?: emptyList()
    }

    val chapterName = remember(chapterPath) {
        chapterPath.substringAfterLast("/").substringAfterLast(":")
    }

    // Callback to refresh folder after deletion
    val onRefresh = {
        viewModel.refreshFolder(folderId)
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
                    // Display Options Button
                    IconButton(onClick = { showDisplayOptions = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Display Options"
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
                        ChapterImageGridView(
                            images = chapterImages,
                            columns = gridColumns, // Dynamic columns
                            onImageClick = onImageClick,
                            onRefresh = onRefresh
                        )
                    } else {
                        ChapterImageListView(
                            images = chapterImages,
                            onImageClick = onImageClick,
                            onRefresh = onRefresh
                        )
                    }
                }
            }
        }

        // Display Options Sheet
        if (showDisplayOptions) {
            DisplayOptionsSheet(
                onDismiss = { showDisplayOptions = false },
                viewMode = null, // Chapters are just images, no sub-view mode
                layoutMode = layoutMode,
                gridColumns = gridColumns,
                sortOption = null, // Hide sort options for chapter images (usually just filename order is desired)
                onLayoutModeChange = { viewModel.setLayoutMode(it) },
                onGridColumnsChange = { viewModel.setGridColumns(it) }
            )
        }
    }
}

@Composable
fun ChapterImageGridView(
    images: List<ImageFile>,
    columns: Int,
    onImageClick: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(images) { index, image ->
            ImageGridItem(
                image = image,
                onClick = { onImageClick(index) },
                onRefresh = onRefresh
            )
        }
    }
}

@Composable
fun ChapterImageListView(
    images: List<ImageFile>,
    onImageClick: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        itemsIndexed(images) { index, image ->
            ChapterImageListItem(
                image = image,
                onClick = { onImageClick(index) },
                onRefresh = onRefresh
            )
        }
    }
}

@Composable
fun ChapterImageListItem(
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
            // Thumbnail
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

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = image.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatSize(image.size)} | ${formatDateString(image.dateModified)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ChapterItemContextMenu(
                    expanded = showMenu,
                    onDismiss = { showMenu = false },
                    onShare = { shareFile(context, image.uri) },
                    onDelete = {
                        if (deleteFile(context, File(image.path))) {
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
fun ChapterItemContextMenu(
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
    }
}

// --- Helpers ---
// Duplicated here for ChapterList specifically or can be sharedUtils
// shareFile and deleteFile logic is identical to ImageGridItem, kept for list items

private fun shareFile(context: android.content.Context, uri: Uri) {
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

private fun deleteFile(context: android.content.Context, file: File): Boolean {
    return try {
        val deleted = file.delete()
        if (deleted) {
            Toast.makeText(context, "Image deleted", Toast.LENGTH_SHORT).show()
            true
        } else {
            Toast.makeText(context, "Failed to delete image", Toast.LENGTH_SHORT).show()
            false
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        false
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun formatDateString(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown Date"
    val sdf = SimpleDateFormat("d MMMM", Locale.getDefault())
    return sdf.format(Date(timestamp))
}