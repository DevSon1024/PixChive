package com.devson.pixchive.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devson.pixchive.ui.components.PermissionDeniedDialog
import com.devson.pixchive.ui.components.PermissionRationaleDialog
import com.devson.pixchive.utils.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val activity = context as? Activity

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

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            permissionState = when {
                isGranted -> PermissionState.Granted
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

    // Request permission function
    val requestPermission: () -> Unit = {
        when {
            PermissionHelper.hasStoragePermission(context) -> {
                permissionState = PermissionState.Granted
            }
            activity != null && PermissionHelper.shouldShowRationale(activity) -> {
                showRationaleDialog = true
            }
            else -> {
                permissionLauncher.launch(PermissionHelper.getStoragePermission())
            }
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
                onClick = {
                    if (permissionState == PermissionState.Granted) {
                        // TODO: Open folder picker (Step 3)
                    } else {
                        requestPermission()
                    }
                },
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
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (permissionState) {
                PermissionState.Granted -> {
                    // Empty state - will show folders in Step 4
                    EmptyStateContent()
                }
                else -> {
                    PermissionRequiredContent(
                        onRequestPermission = requestPermission
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

@Composable
fun PermissionRequiredContent(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔒",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Storage Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Grant storage access to view your comic images",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}