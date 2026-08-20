package com.devson.pixchive.feature.reader.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.devson.pixchive.core.data.FolderWithCover
import com.devson.pixchive.core.data.local.HistoryEntity
import com.devson.pixchive.core.designsystem.component.*
import com.devson.pixchive.core.utils.PermissionHelper
import com.devson.pixchive.core.utils.PermissionState
import com.devson.pixchive.feature.home.HomeViewModel
import com.devson.pixchive.feature.reader.ui.components.FolderCard
import com.devson.pixchive.feature.reader.ui.components.FolderGridItem

/**
 * Dedicated Comic and Manga Library Screen displaying all imported root folders.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onFolderClick: (String) -> Unit,
    onSettingsClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onResumeChapter: (folderId: String, chapterPath: String, initialPage: Int) -> Unit = { _, _, _ -> },
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val folders by viewModel.folders.collectAsState()
    val recentHistory by viewModel.recentHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val favoriteCount by viewModel.favoriteCount.collectAsState()

    val layoutMode by viewModel.layoutMode.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()

    val gridState = rememberLazyGridState()
    val isFabExpanded by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset < 100
        }
    }

    var permissionState by remember { mutableStateOf<PermissionState>(PermissionState.NotRequested) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDisplayOptions by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionState = if (PermissionHelper.hasStoragePermission(context)) {
                    PermissionState.Granted
                } else {
                    PermissionState.NotRequested
                }
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
                if (activity != null && PermissionHelper.shouldShowRationale(activity)) {
                    showRationaleDialog = true
                } else {
                    showSettingsDialog = true
                }
            }
        }
    )

    val allFilesAccessLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (PermissionHelper.hasStoragePermission(context)) {
            permissionState = PermissionState.Granted
            folderPickerLauncher.launch(null)
        } else {
            showSettingsDialog = true
        }
    }

    val requestPermissionAndOpenPicker: () -> Unit = {
        if (PermissionHelper.hasStoragePermission(context)) {
            permissionState = PermissionState.Granted
            folderPickerLauncher.launch(null)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                showRationaleDialog = true
            } else {
                if (activity != null && PermissionHelper.shouldShowRationale(activity)) {
                    showRationaleDialog = true
                } else {
                    legacyPermissionLauncher.launch(PermissionHelper.getLegacyStoragePermission())
                }
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
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Library",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        if (isSyncing) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.height(24.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Syncing",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshFolders() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showDisplayOptions = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Display Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onFavoritesClick) {
                        BadgedBox(
                            badge = {
                                if (favoriteCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text("$favoriteCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorites",
                                tint = if (favoriteCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = requestPermissionAndOpenPicker,
                expanded = isFabExpanded,
                icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp)) },
                text = { Text("Add Folder", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 96.dp)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            when {
                isLoading && folders.isEmpty() -> {
                    SkeletonLoadingView(
                        layoutMode = layoutMode,
                        columns = gridColumns,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                folders.isEmpty() -> {
                    EmptyFoldersView(onAddFolderClick = requestPermissionAndOpenPicker)
                }
                else -> {
                    var localColumns by remember(gridColumns) { mutableIntStateOf(gridColumns) }
                    var accumulatedZoom by remember { mutableFloatStateOf(1f) }

                    val animatedColumns by animateIntAsState(
                        targetValue = localColumns,
                        animationSpec = tween(300),
                        label = "lib_columns_anim"
                    )

                    val gridCols = if (layoutMode == "grid") animatedColumns.coerceIn(1, 4) else 1

                    val zoomModifier = if (layoutMode == "grid") {
                        Modifier.pointerInput(Unit) {
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
                                                val newCols = (localColumns - 1).coerceIn(1, 4)
                                                if (newCols != localColumns) {
                                                    localColumns = newCols
                                                    viewModel.setGridColumns(newCols)
                                                }
                                                hasChangedInThisGesture = true
                                            } else if (accumulatedZoom < 0.75f) {
                                                val newCols = (localColumns + 1).coerceIn(1, 4)
                                                if (newCols != localColumns) {
                                                    localColumns = newCols
                                                    viewModel.setGridColumns(newCols)
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
                    } else Modifier

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refreshFolders() },
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (layoutMode == "grid") {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(gridCols),
                                state = gridState,
                                contentPadding = PaddingValues(
                                    top = 8.dp,
                                    bottom = 100.dp,
                                    start = 8.dp,
                                    end = 8.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(zoomModifier)
                            ) {
                                if (recentHistory.isNotEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        ContinueReadingSection(
                                            recentHistory = recentHistory.take(3),
                                            folders = folders,
                                            onResumeChapter = onResumeChapter
                                        )
                                    }
                                }

                                items(folders, key = { it.id }, contentType = { "folder_grid" }) { folderItem ->
                                    FolderGridItem(
                                        folderWithCover = folderItem,
                                        onDelete = { viewModel.removeFolder(folderItem.id) },
                                        onClick = { onFolderClick(folderItem.id) }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    top = 8.dp,
                                    bottom = 100.dp,
                                    start = 16.dp,
                                    end = 16.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (recentHistory.isNotEmpty()) {
                                    item {
                                        ContinueReadingSection(
                                            recentHistory = recentHistory.take(3),
                                            folders = folders,
                                            onResumeChapter = onResumeChapter
                                        )
                                    }
                                }

                                items(folders, key = { it.id }, contentType = { "folder_list" }) { folderItem ->
                                    FolderCard(
                                        folderWithCover = folderItem,
                                        onDelete = { viewModel.removeFolder(folderItem.id) },
                                        onClick = { onFolderClick(folderItem.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Display Options Bottom Sheet
        if (showDisplayOptions) {
            ViewSettingsBottomSheet(
                onDismiss = { showDisplayOptions = false },
                layoutMode = layoutMode,
                gridColumns = gridColumns,
                sortOption = sortOption,
                onLayoutModeChange = { viewModel.setLayoutMode(it) },
                onGridColumnsChange = { viewModel.setGridColumns(it) },
                onSortOptionChange = { viewModel.setSortOption(it) }
            )
        }

        // Permission Rationale / Denied Dialogs
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

@Composable
private fun ContinueReadingSection(
    recentHistory: List<HistoryEntity>,
    folders: List<FolderWithCover>,
    onResumeChapter: (folderId: String, chapterPath: String, initialPage: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Continue Reading",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(recentHistory, key = { it.chapterPath }) { entry ->
                val mainFolderName = folders.find { it.id == entry.folderId }?.displayName ?: ""
                CompactContinueReadingCard(
                    entry = entry,
                    mainFolderName = mainFolderName,
                    onClick = {
                        onResumeChapter(entry.folderId, entry.chapterPath, entry.currentPage)
                    }
                )
            }
        }
    }
}

@Composable
private fun CompactContinueReadingCard(
    entry: HistoryEntity,
    mainFolderName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier
            .width(230.dp)
            .height(82.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                if (entry.coverImageUri.isNotEmpty()) {
                    val context = LocalContext.current
                    val coverRequest = remember(entry.coverImageUri) {
                        ImageRequest.Builder(context)
                            .data(entry.coverImageUri)
                            .size(160)
                            .crossfade(false)
                            .bitmapConfig(Bitmap.Config.RGB_565)
                            .allowHardware(true)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build()
                    }
                    AsyncImage(
                        model = coverRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                if (mainFolderName.isNotBlank()) {
                    Text(
                        text = mainFolderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val progress = if (entry.totalPages > 0) {
                    (entry.currentPage + 1f) / entry.totalPages.toFloat()
                } else 0f
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Page ${entry.currentPage + 1}/${entry.totalPages.coerceAtLeast(1)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
