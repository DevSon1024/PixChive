package com.devson.pixchive.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.data.ComicFolder
import com.devson.pixchive.ui.components.PermissionDeniedDialog
import com.devson.pixchive.ui.components.PermissionRationaleDialog
import com.devson.pixchive.utils.PermissionHelper
import com.devson.pixchive.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onFolderClick: (String) -> Unit = { _ -> },
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val folders by viewModel.folders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val layoutMode by viewModel.layoutMode.collectAsState()
    val currentSortOption by viewModel.sortOption.collectAsState()

    var permissionState by remember { mutableStateOf<PermissionState>(PermissionState.NotRequested) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Check permission on resume
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

    // Permission Launchers
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            uri?.let {
                try {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
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
        onResult = { isGranted: Boolean ->
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

    val allFilesAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
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
                title = { Text("PixChive") },
                actions = {
                    // Sort Menu
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Date (Newest First)") },
                                onClick = {
                                    viewModel.setSortOption("date_newest")
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (currentSortOption == "date_newest") {
                                        Icon(Icons.Default.Check, null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Date (Oldest First)") },
                                onClick = {
                                    viewModel.setSortOption("date_oldest")
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (currentSortOption == "date_oldest") {
                                        Icon(Icons.Default.Check, null)
                                    }
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Name (A-Z)") },
                                onClick = {
                                    viewModel.setSortOption("name_asc")
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (currentSortOption == "name_asc") {
                                        Icon(Icons.Default.Check, null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (Z-A)") },
                                onClick = {
                                    viewModel.setSortOption("name_desc")
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (currentSortOption == "name_desc") {
                                        Icon(Icons.Default.Check, null)
                                    }
                                }
                            )
                        }
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

                    // Settings
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
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Folder",
                    modifier = Modifier.size(32.dp)
                )
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
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                folders.isEmpty() -> {
                    EmptyStateContent()
                }
                else -> {
                    if (layoutMode == "grid") {
                        FolderGridContent(
                            folders = folders,
                            onDeleteFolder = { viewModel.removeFolder(it) },
                            onFolderClick = onFolderClick
                        )
                    } else {
                        FolderListContent(
                            folders = folders,
                            onDeleteFolder = { viewModel.removeFolder(it) },
                            onFolderClick = onFolderClick
                        )
                    }
                }
            }
        }

        // Dialogs
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
fun FolderListContent(
    folders: List<ComicFolder>,
    onDeleteFolder: (String) -> Unit,
    onFolderClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(folders, key = { it.id }) { folder ->
            FolderCard(
                folder = folder,
                onDelete = { onDeleteFolder(folder.id) },
                onClick = { onFolderClick(folder.id) }
            )
        }
    }
}

@Composable
fun FolderGridContent(
    folders: List<ComicFolder>,
    onDeleteFolder: (String) -> Unit,
    onFolderClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(folders, key = { it.id }) { folder ->
            FolderGridItem(
                folder = folder,
                onDelete = { onDeleteFolder(folder.id) },
                onClick = { onFolderClick(folder.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderCard(
    folder: ComicFolder,
    onDelete: () -> Unit,
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
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${folder.chapterCount} chapters • ${folder.imageCount} images",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete folder",
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
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // Square card
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = folder.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${folder.imageCount} imgs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Context Menu for Grid Item
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Delete Folder", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    showMenu = false
                    onDelete()
                }
            )
        }
    }
}

@Composable
fun EmptyStateContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📚",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Folders Added",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the + button to add your comic folders",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}