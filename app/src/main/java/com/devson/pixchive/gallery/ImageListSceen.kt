package com.devson.pixchive.gallery

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.gallery.data.models.GalleryFolder
import com.devson.pixchive.gallery.ui.components.GalleryFolderItem
import com.devson.pixchive.gallery.ui.components.FolderDetailsDialog
import com.devson.pixchive.gallery.viewmodel.GalleryState
import com.devson.pixchive.gallery.viewmodel.ImageListViewModel
import com.devson.pixchive.utils.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageListScreen(
    onNavigateBack: () -> Unit,
    onFolderClick: (String) -> Unit,
    viewModel: ImageListViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Permission State using your existing PermissionHelper
    var hasPermission by remember { mutableStateOf(PermissionHelper.hasStoragePermission(context)) }
    var selectedFolderForDetails by remember { mutableStateOf<GalleryFolder?>(null) }

    // Re-check permission when resuming the screen (important if they come back from settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentlyHasPermission = PermissionHelper.hasStoragePermission(context)
                if (currentlyHasPermission && !hasPermission) {
                    hasPermission = true
                    viewModel.loadGalleryFolders()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Launcher for Android 10 and below (Runtime Permission)
    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (isGranted) viewModel.loadGalleryFolders()
        }
    )

    // Launcher for Android 11+ (Settings Intent for "All Files Access")
    val allFilesAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            val isGranted = PermissionHelper.hasStoragePermission(context)
            hasPermission = isGranted
            if (isGranted) viewModel.loadGalleryFolders()
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Gallery") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!hasPermission) {
                // UI when permission is missing
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Lock, 
                        contentDescription = null, 
                        modifier = Modifier.size(64.dp), 
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Storage Permission Required", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "PixChive needs access to your storage to find device images.", 
                        textAlign = TextAlign.Center, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // FIXED BUTTON: Uses PermissionHelper appropriately
                    Button(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try {
                                allFilesAccessLauncher.launch(PermissionHelper.getStoragePermissionSettingsIntent(context))
                            } catch (e: Exception) {
                                // Fallback if settings can't open
                            }
                        } else {
                            legacyPermissionLauncher.launch(PermissionHelper.getLegacyStoragePermission())
                        }
                    }) {
                        Text("Grant Permission")
                    }
                }
            } else {
                // UI when we have permission and are loading data
                when (val state = uiState) {
                    is GalleryState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is GalleryState.Error -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                    is GalleryState.Success -> {
                        if (state.folders.isEmpty()) {
                            Text(
                                text = "No images found on this device.",
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 120.dp),
                                contentPadding = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(state.folders, key = { it.bucketId }) { folder ->
                                    GalleryFolderItem(
                                        folder = folder,
                                        onClick = { onFolderClick(folder.bucketId) },
                                        onLongPress = { selectedFolderForDetails = folder }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Show Real Path Dialog on Long Press
        selectedFolderForDetails?.let { folder ->
            FolderDetailsDialog(
                folder = folder,
                onDismiss = { selectedFolderForDetails = null }
            )
        }
    }
}