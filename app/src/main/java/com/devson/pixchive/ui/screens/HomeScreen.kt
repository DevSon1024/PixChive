package com.devson.pixchive.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.devson.pixchive.data.ComicFolder
import com.devson.pixchive.data.local.HistoryEntity
import com.devson.pixchive.ui.components.DisplayOptionsSheet
import com.devson.pixchive.ui.components.PermissionDeniedDialog
import com.devson.pixchive.ui.components.PermissionRationaleDialog
import com.devson.pixchive.ui.components.SkeletonGrid
import com.devson.pixchive.ui.components.SkeletonList
import com.devson.pixchive.utils.PermissionHelper
import com.devson.pixchive.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onFolderClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onResumeChapter: (folderId: String, chapterPath: String, initialPage: Int) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val folders by viewModel.folders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val recentHistory by viewModel.recentHistory.collectAsState()

    val layoutMode by viewModel.layoutMode.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()

    var permissionState by remember { mutableStateOf<PermissionState>(PermissionState.NotRequested) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDisplayOptions by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionState = if (PermissionHelper.hasStoragePermission(context)) PermissionState.Granted else PermissionState.NotRequested
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            uri?.let {
                try {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(it, takeFlags)
                    val folderName = it.lastPathSegment?.substringAfterLast(':') ?: "Unknown Folder"
                    viewModel.addFolder(it, folderName)
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to access folder: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    )

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                permissionState = PermissionState.Granted
                folderPickerLauncher.launch(null)
            } else {
                if (activity != null && PermissionHelper.shouldShowRationale(activity)) showRationaleDialog = true
                else showSettingsDialog = true
            }
        }
    )

    val allFilesAccessLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (PermissionHelper.hasStoragePermission(context)) {
            permissionState = PermissionState.Granted
            folderPickerLauncher.launch(null)
        } else showSettingsDialog = true
    }

    val requestPermissionAndOpenPicker: () -> Unit = {
        if (PermissionHelper.hasStoragePermission(context)) {
            permissionState = PermissionState.Granted
            folderPickerLauncher.launch(null)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) showRationaleDialog = true
            else {
                if (activity != null && PermissionHelper.shouldShowRationale(activity)) showRationaleDialog = true
                else legacyPermissionLauncher.launch(PermissionHelper.getLegacyStoragePermission())
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PixChive") },
                actions = {
                    IconButton(onClick = onFavoritesClick) {
                        Icon(Icons.Default.Favorite, contentDescription = "Favorites", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = { showDisplayOptions = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Display Options")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = requestPermissionAndOpenPicker,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Add Folder", Modifier.size(32.dp))
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    if (layoutMode == "grid") SkeletonGrid(columns = gridColumns) else SkeletonList()
                }
                folders.isEmpty() -> EmptyStateContent()
                else -> {
                    // ── Single LazyVerticalGrid that holds EVERYTHING: history row + folders ──
                    // Using item(span) for full-width header/row items avoids nesting a
                    // LazyColumn + LazyVerticalGrid (which crashes Compose).
                    val gridCols = if (layoutMode == "grid") gridColumns else 1

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridCols),
                        contentPadding = PaddingValues(bottom = 88.dp), // FAB clearance
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // ── HISTORY SECTION ──────────────────────────────────────────────
                        if (recentHistory.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SectionHeader(
                                    title = "Jump Back In",
                                    icon = Icons.Default.History
                                )
                            }

                            item(span = { GridItemSpan(maxLineSpan) }) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(recentHistory, key = { it.chapterPath }) { entry ->
                                        HistoryCard(
                                            entry = entry,
                                            onClick = {
                                                onResumeChapter(entry.folderId, entry.chapterPath, entry.currentPage)
                                            }
                                        )
                                    }
                                }
                            }

                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        // ── FOLDERS SECTION ──────────────────────────────────────────────
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader(
                                title = "My Folders",
                                icon = Icons.Default.FolderOpen
                            )
                        }

                        if (layoutMode == "grid") {
                            items(folders, key = { it.id }, contentType = { "folder_grid" }) { folder ->
                                Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    FolderGridItem(
                                        folder = folder,
                                        onDelete = { viewModel.removeFolder(folder.id) },
                                        onClick = { onFolderClick(folder.id) }
                                    )
                                }
                            }
                        } else {
                            items(folders, key = { it.id }, contentType = { "folder_list" }) { folder ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                    FolderCard(
                                        folder = folder,
                                        onDelete = { viewModel.removeFolder(folder.id) },
                                        onClick = { onFolderClick(folder.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Syncing Chip ──────────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = isSyncing,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    tonalElevation = 4.dp,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Syncing folders...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
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

        if (showRationaleDialog) {
            PermissionRationaleDialog(
                rationale = PermissionHelper.getPermissionRationale(),
                onConfirm = {
                    showRationaleDialog = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            allFilesAccessLauncher.launch(PermissionHelper.getStoragePermissionSettingsIntent(context))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        legacyPermissionLauncher.launch(PermissionHelper.getLegacyStoragePermission())
                    }
                },
                onDismiss = { showRationaleDialog = false }
            )
        }

        if (showSettingsDialog) {
            PermissionDeniedDialog(
                onOpenSettings = {
                    showSettingsDialog = false
                    try {
                        context.startActivity(PermissionHelper.getStoragePermissionSettingsIntent(context))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

// ── Section Header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── History Card ──────────────────────────────────────────────────────────────

@Composable
fun HistoryCard(entry: HistoryEntity, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .aspectRatio(0.7f),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Cover image
            AsyncImage(
                model = entry.coverImageUri,
                contentDescription = entry.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient overlay + text at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Column {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Progress bar at absolute bottom
                    LinearProgressIndicator(
                        progress = { (entry.currentPage.toFloat() / entry.totalPages.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            // Page badge top-right
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                Text(
                    text = "At ${entry.currentPage + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// Folder Views (preserved)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderCard(folder: ComicFolder, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Folder, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(folder.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text("${folder.chapterCount} chapters • ${folder.imageCount} images", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderGridItem(folder: ComicFolder, onDelete: () -> Unit, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); showMenu = true }
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Folder, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(folder.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                Text("${folder.imageCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = { showMenu = false; onDelete() },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}

@Composable
fun EmptyStateContent() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📚", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No Folders Added", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Tap the + button to add your comic folders", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}