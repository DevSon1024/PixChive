package com.devson.pixchive.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.R
import com.devson.pixchive.gallery.ui.components.CustomRenameDialog
import com.devson.pixchive.viewmodel.FileOperationsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageExplorerScreen(
    operationType: String,
    sourceUris: List<Uri>,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    fileOpsViewModel: FileOperationsViewModel = viewModel()
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        StorageExplorerContent(
            operationType = operationType,
            sourceUris = sourceUris,
            onComplete = onComplete,
            onCancel = onCancel,
            fileOpsViewModel = fileOpsViewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StorageExplorerContent(
    operationType: String,
    sourceUris: List<Uri>,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    fileOpsViewModel: FileOperationsViewModel
) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                Environment.isExternalStorageManager()
            else
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }
    val manageStorageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            hasPermission = Environment.isExternalStorageManager()
    }

    fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                manageStorageLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                )
            } catch (e: Exception) {
                manageStorageLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    LaunchedEffect(Unit) { if (!hasPermission) requestPermission() }

    if (!hasPermission) {
        PermissionScreen(operationType = operationType, onRequest = ::requestPermission, onCancel = onCancel)
        return
    }

    val rootDir = remember { Environment.getExternalStorageDirectory() }
    var currentDirectory by remember { mutableStateOf(rootDir) }
    var folders by remember { mutableStateOf(emptyList<File>()) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    fun refreshFolders() {
        folders = if (currentDirectory.exists() && currentDirectory.isDirectory)
            currentDirectory.listFiles()
                ?.filter { it.isDirectory && !it.isHidden }
                ?.sortedBy { it.name.lowercase() }
                ?: emptyList()
        else emptyList()
    }

    LaunchedEffect(currentDirectory) { refreshFolders() }

    val operationInProgress by fileOpsViewModel.operationInProgress.collectAsState()
    val opResult by fileOpsViewModel.operationResult.collectAsState()

    LaunchedEffect(opResult) { if (opResult != null) onComplete() }

    val isAtRoot = currentDirectory.absolutePath == rootDir.absolutePath

    BackHandler {
        if (!isAtRoot) currentDirectory = currentDirectory.parentFile ?: rootDir
        else onCancel()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isAtRoot) stringResource(R.string.storage_internal_storage) else currentDirectory.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!isAtRoot) currentDirectory = currentDirectory.parentFile ?: rootDir
                        else onCancel()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateFolderDialog = true }) {
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = stringResource(R.string.cd_create_new_folder))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    if (operationInProgress) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (operationType == "MOVE") stringResource(R.string.storage_moving)
                                else stringResource(R.string.storage_copying),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.storage_items_count, sourceUris.size),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onCancel,
                                enabled = !operationInProgress
                            ) {
                                Text(stringResource(R.string.history_cancel_button))
                            }
                            Button(
                                onClick = {
                                    if (operationType == "MOVE")
                                        fileOpsViewModel.moveItemsToPath(context, sourceUris, currentDirectory)
                                    else
                                        fileOpsViewModel.copyItemsToPath(context, sourceUris, currentDirectory)
                                },
                                enabled = !operationInProgress
                            ) {
                                Text(stringResource(R.string.storage_paste_here))
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding()
                )
        ) {
            BreadcrumbRow(
                currentDirectory = currentDirectory,
                rootDir = rootDir,
                onNavigate = { currentDirectory = it }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(folders, key = { it.absolutePath }) { folder ->
                    FolderRow(folder = folder, onClick = { currentDirectory = folder })
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
                if (folders.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.storage_empty_folder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        CustomRenameDialog(
            initialName = stringResource(R.string.storage_new_folder),
            title = stringResource(R.string.storage_new_folder),
            onConfirm = { name ->
                val newFolder = File(currentDirectory, name)
                if (!newFolder.exists()) {
                    newFolder.mkdir()
                    refreshFolders()
                }
                showCreateFolderDialog = false
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }
}

@Composable
private fun BreadcrumbRow(
    currentDirectory: File,
    rootDir: File,
    onNavigate: (File) -> Unit
) {
    val segments = remember(currentDirectory) {
        val list = mutableListOf<Pair<String, File>>()
        var dir: File? = currentDirectory
        while (dir != null && dir.absolutePath.startsWith(rootDir.absolutePath)) {
            list.add(0, (if (dir.absolutePath == rootDir.absolutePath) "Internal" else dir.name) to dir)
            if (dir.absolutePath == rootDir.absolutePath) break
            dir = dir.parentFile
        }
        list
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(segments.indices.toList()) { index ->
            val (label, file) = segments[index]
            val isLast = index == segments.size - 1
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .then(if (!isLast) Modifier.clickable { onNavigate(file) } else Modifier)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            if (!isLast) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

@Composable
private fun FolderRow(folder: File, onClick: () -> Unit) {
    val subFolderCount = remember(folder) { folder.listFiles()?.count { it.isDirectory } ?: 0 }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = folder.name, style = MaterialTheme.typography.bodyLarge)
            if (subFolderCount > 0) {
                Text(
                    text = "$subFolderCount subfolder${if (subFolderCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionScreen(
    operationType: String,
    onRequest: () -> Unit,
    onCancel: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.storage_permission_required,
                    if (operationType == "MOVE") stringResource(R.string.storage_move)
                    else stringResource(R.string.storage_copy)
                ),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequest) { Text(stringResource(R.string.storage_grant_permission)) }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onCancel) { Text(stringResource(R.string.history_cancel_button)) }
        }
    }
}