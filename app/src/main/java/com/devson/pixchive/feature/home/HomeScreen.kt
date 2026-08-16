package com.devson.pixchive.feature.home

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devson.pixchive.core.data.ComicFolder
import com.devson.pixchive.core.data.PreferencesManager
import com.devson.pixchive.core.data.local.HistoryEntity
import com.devson.pixchive.core.data.local.ImageEntity
import com.devson.pixchive.core.designsystem.component.*
import com.devson.pixchive.core.utils.PermissionHelper
import com.devson.pixchive.core.utils.PermissionState
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onFolderClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onResumeChapter: (folderId: String, chapterPath: String, initialPage: Int) -> Unit = { _, _, _ -> },
    onBrowseGalleryClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val folders by viewModel.folders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val recentHistory by viewModel.recentHistory.collectAsState()
    val favoriteCount by viewModel.favoriteCount.collectAsState()

    val layoutMode by viewModel.layoutMode.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val galleryViewMode by viewModel.galleryViewMode.collectAsState()

    val preferencesManager = remember { PreferencesManager(context) }
    val showHistory by preferencesManager.showHistoryFlow.collectAsState(initial = true)
    val showFolderCard by preferencesManager.showFolderCardFlow.collectAsState(initial = true)

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
                            text = "PixChive",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
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
                    // Favorites action with badge if count > 0
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

                    if (showFolderCard) {
                        IconButton(onClick = { showDisplayOptions = true }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Display Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                        SkeletonHome(
                            layoutMode = layoutMode,
                            columns = gridColumns,
                            showHistory = recentHistory.isNotEmpty()
                        )
                    }
                }
                folders.isEmpty() && recentHistory.isEmpty() -> {
                    Box(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                        EmptyFoldersView(onAddFolderClick = requestPermissionAndOpenPicker)
                    }
                }
                else -> {
                    var localColumns by remember(gridColumns) { mutableIntStateOf(gridColumns) }
                    var accumulatedZoom by remember { mutableFloatStateOf(1f) }

                    val animatedColumns by animateIntAsState(
                        targetValue = localColumns,
                        animationSpec = tween(300),
                        label = "columns_anim"
                    )

                    val gridCols = if (layoutMode == "grid") animatedColumns.coerceIn(1, 6) else 1

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
                                                val newCols = (localColumns - 1).coerceIn(1, 6)
                                                if (newCols != localColumns) {
                                                    localColumns = newCols
                                                    viewModel.setGridColumns(newCols)
                                                }
                                                hasChangedInThisGesture = true
                                            } else if (accumulatedZoom < 0.75f) {
                                                val newCols = (localColumns + 1).coerceIn(1, 6)
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
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridCols),
                            state = gridState,
                            contentPadding = PaddingValues(
                                top = paddingValues.calculateTopPadding() + 8.dp,
                                bottom = paddingValues.calculateBottomPadding() + 88.dp
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .then(zoomModifier)
                        ) {
                            // BROWSE GALLERY ENTRY CARD
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                OutlinedCard(
                                    onClick = { onBrowseGalleryClick(galleryViewMode) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoLibrary,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Browse Gallery",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Explore all photos and albums",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            // JUMP BACK IN / HISTORY SECTION
                            if (showHistory && recentHistory.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    HomeSectionHeader(
                                        title = "Jump Back In",
                                        icon = Icons.Default.History
                                    )
                                }

                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    val carouselState = rememberCarouselState { recentHistory.size }
                                    HorizontalMultiBrowseCarousel(
                                        state = carouselState,
                                        preferredItemWidth = 150.dp,
                                        itemSpacing = 12.dp,
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(210.dp)
                                    ) { index ->
                                        val entry = recentHistory[index]
                                        val mainFolderName = folders.find { it.id == entry.folderId }?.displayName ?: ""
                                        HistoryCard(
                                            entry = entry,
                                            mainFolderName = mainFolderName,
                                            onClick = {
                                                onResumeChapter(entry.folderId, entry.chapterPath, entry.currentPage)
                                            },
                                            onDeleteClick = {
                                                viewModel.removeHistoryItem(entry.folderId, entry.chapterPath)
                                            },
                                            onGoToFolder = {
                                                onFolderClick(entry.folderId)
                                            },
                                            modifier = Modifier
                                                .height(210.dp)
                                                .maskClip(RoundedCornerShape(16.dp))
                                        )
                                    }
                                }

                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            // MY FOLDERS SECTION
                            if (showFolderCard && folders.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    HomeSectionHeader(
                                        title = "My Folders",
                                        icon = Icons.Default.FolderOpen
                                    )
                                }

                                if (layoutMode == "grid") {
                                    items(folders, key = { it.id }, contentType = { "folder_grid" }) { folder ->
                                        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                            FolderGridItem(
                                                folder = folder,
                                                latestImageFlow = remember(folder.id) { viewModel.getLatestImageFlow(folder.id) },
                                                onDelete = { viewModel.removeFolder(folder.id) },
                                                onClick = { onFolderClick(folder.id) }
                                            )
                                        }
                                    }
                                } else {
                                    items(folders, key = { it.id }, contentType = { "folder_list" }) { folder ->
                                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                            FolderCard(
                                                folder = folder,
                                                latestImageFlow = remember(folder.id) { viewModel.getLatestImageFlow(folder.id) },
                                                onDelete = { viewModel.removeFolder(folder.id) },
                                                onClick = { onFolderClick(folder.id) }
                                            )
                                        }
                                    }
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
private fun HomeSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.15.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryCard(
    entry: HistoryEntity,
    mainFolderName: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onGoToFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Cover image
                AsyncImage(
                    model = entry.coverImageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark gradient scrim from bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Black.copy(alpha = 0.85f)
                                ),
                                startY = 60f
                            )
                        )
                )

                // Page count badge at top right
                if (entry.totalPages > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "${entry.currentPage + 1}/${entry.totalPages}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Bottom details
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    if (mainFolderName.isNotBlank()) {
                        Text(
                            text = mainFolderName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val progress = if (entry.totalPages > 0) {
                        (entry.currentPage + 1f) / entry.totalPages.toFloat()
                    } else 0f

                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f),
                    )
                }
            }
        }

        if (showMenu) {
            OptionsBottomSheet(
                title = entry.title,
                subtitle = mainFolderName.ifBlank { null },
                options = listOf(
                    OptionItem(
                        label = "Go to Folder",
                        icon = Icons.Default.FolderOpen,
                        onClick = onGoToFolder
                    ),
                    OptionItem(
                        label = "Remove from History",
                        icon = Icons.Default.Delete,
                        isDestructive = true,
                        onClick = onDeleteClick
                    )
                ),
                onDismiss = { showMenu = false }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCard(
    folder: ComicFolder,
    latestImageFlow: Flow<ImageEntity?>,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val latestImage by latestImageFlow.collectAsState(initial = null)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (latestImage != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(latestImage!!.uri)
                            .size(256)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${folder.chapterCount} chapters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${folder.imageCount} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderGridItem(
    folder: ComicFolder,
    latestImageFlow: Flow<ImageEntity?>,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }
    val latestImage by latestImageFlow.collectAsState(initial = null)

    Box {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (latestImage != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(latestImage!!.uri)
                            .size(256)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .combinedClickable(
                            onClick = onClick,
                            onLongClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                showMenu = true
                            }
                        )
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (latestImage != null) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = if (latestImage != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = folder.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (latestImage != null) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${folder.imageCount} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (latestImage != null) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        if (showMenu) {
            OptionsBottomSheet(
                title = folder.displayName,
                subtitle = "${folder.imageCount} items",
                options = listOf(
                    OptionItem(
                        label = "Delete",
                        icon = Icons.Default.Delete,
                        isDestructive = true,
                        onClick = onDelete
                    )
                ),
                onDismiss = { showMenu = false }
            )
        }
    }
}