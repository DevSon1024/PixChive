package com.devson.pixchive.gallery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.gallery.data.models.GalleryImage
import com.devson.pixchive.gallery.ui.components.DetailsDialog
import com.devson.pixchive.gallery.ui.components.GalleryBottomOptionSheet
import com.devson.pixchive.gallery.ui.components.GalleryImageItem
import com.devson.pixchive.gallery.ui.components.GalleryViewSettingsBottomSheet
import com.devson.pixchive.gallery.viewmodel.GalleryFolderViewModel

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ImageFolderScreen(
    bucketId: String,
    onNavigateBack: () -> Unit,
    onImageClick: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: GalleryFolderViewModel = viewModel()
) {
    val images by viewModel.images.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val selectedImageIds = remember { mutableStateListOf<Long>() }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedImageIds.isNotEmpty()) {
        selectedImageIds.clear()
    }

    LaunchedEffect(bucketId) {
        viewModel.loadImages(bucketId)
    }

    val selectedImages: List<GalleryImage> = if (showDetailsDialog) {
        images.filter { it.id in selectedImageIds }
    } else emptyList()

    Scaffold(
        topBar = {
            if (selectedImageIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text("${selectedImageIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectedImageIds.clear() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showOptionsSheet = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            } else {
                TopAppBar(
                    title = { Text("Folder Images") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "View Settings")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (images.isEmpty()) {
                Text("No images found.", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(images, key = { _, img -> img.id }) { index, image ->
                        val sharedModifier = with(sharedTransitionScope) {
                            Modifier.sharedElement(
                                sharedContentState = rememberSharedContentState(key = "image_${image.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                        GalleryImageItem(
                            image = image,
                            isSelected = image.id in selectedImageIds,
                            modifier = sharedModifier,
                            onClick = {
                                if (selectedImageIds.isNotEmpty()) {
                                    if (image.id in selectedImageIds) selectedImageIds.remove(image.id)
                                    else selectedImageIds.add(image.id)
                                } else {
                                    onImageClick(index)
                                }
                            },
                            onLongClick = {
                                if (image.id !in selectedImageIds) selectedImageIds.add(image.id)
                                showOptionsSheet = true
                            }
                        )
                    }
                }
            }
        }

        if (showOptionsSheet) {
            GalleryBottomOptionSheet(
                selectedCount = selectedImageIds.size,
                onDismiss = { showOptionsSheet = false },
                onMove = {},
                onCopy = {},
                onDelete = {},
                onRename = {},
                onShare = {},
                onInfo = {
                    showOptionsSheet = false
                    showDetailsDialog = true
                }
            )
        }

        if (showSettingsSheet) {
            GalleryViewSettingsBottomSheet(onDismiss = { showSettingsSheet = false })
        }

        if (showDetailsDialog) {
            DetailsDialog(
                selectedFolders = emptyList(),
                selectedImages = selectedImages,
                onDismiss = { showDetailsDialog = false }
            )
        }
    }
}