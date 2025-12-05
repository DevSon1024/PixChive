package com.devson.pixchive.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    onFolderClick: ( String) -> Unit = { _ -> }
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val folders by viewModel.folders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var permissionState by remember {
        mutableStateOf<PermissionState>(
            if (PermissionHelper.hasStoragePermission(context)) {
                PermissionState.Granted
            } else {
                PermissionState.NotRequested
            }
        )
    }

    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            uri?.let {
                // Persist permissions
                val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)

                // Get folder name
                val folderName = it.lastPathSegment?.substringAfterLast(':') ?: "Unknown Folder"
                viewModel.addFolder(it, folderName)
            }
        }
    )

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            permissionState = when {
                isGranted -> {
                    folderPickerLauncher.launch(null)
                    PermissionState.Granted
                }
                activity != null && PermissionHelper.shouldShowRationale(activity) -> {
                    PermissionState.Denied
                }
                else -> {
                    showSettingsDialog = true
                    PermissionState.PermanentlyDenied
                }
            }
        }
    )

    // Request permission and open folder picker
    val requestPermissionAndOpenPicker: () -> Unit = {
        when {
            PermissionHelper.hasStoragePermission(context) -> {
                permissionState = PermissionState.Granted
                folderPickerLauncher.launch(null)
            }
            activity != null && PermissionHelper.shouldShowRationale(activity) -> {
                showRationaleDialog = true
            }
            else -> {
                permissionLauncher.launch(PermissionHelper.getStoragePermission())
            }
        }
    }

    // Show error messages
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
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                folders.isEmpty() -> {
                    EmptyStateContent()
                }
                else -> {
                    FolderListContent(
                        folders = folders,
                        onDeleteFolder = { folderId ->
                            viewModel.removeFolder(folderId)
                        },
                        onFolderClick = onFolderClick
                    )
                }
            }
        }

        // Show rationale dialog
        if (showRationaleDialog) {
            PermissionRationaleDialog(
                rationale = PermissionHelper.getPermissionRationale(),
                onConfirm = {
                    showRationaleDialog = false
                    permissionLauncher.launch(PermissionHelper.getStoragePermission())
                },
                onDismiss = {
                    showRationaleDialog = false
                }
            )
        }

        // Show settings dialog
        if (showSettingsDialog) {
            PermissionDeniedDialog(
                onOpenSettings = {
                    showSettingsDialog = false
                    PermissionHelper.openAppSettings(context)
                },
                onDismiss = {
                    showSettingsDialog = false
                }
            )
        }
    }
}

@Composable
fun FolderListContent(
    folders: List<ComicFolder>,
    onDeleteFolder: (String) -> Unit,
    // CHANGED: Callback signature
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
                // CHANGED: Just pass ID, don't force "explorer"
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