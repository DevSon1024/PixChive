package com.devson.pixchive.gallery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.devson.pixchive.gallery.viewmodel.GalleryFolderViewModel
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope

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

    LaunchedEffect(bucketId) {
        viewModel.loadImages(bucketId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Folder Images") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
                        with(sharedTransitionScope) {
                            AsyncImage(
                                model = image.uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState(key = "image_${image.id}"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                    .clickable { onImageClick(index) }
                            )
                        }
                    }
                }
            }
        }
    }
}