package com.devson.pixchive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.devson.pixchive.viewmodel.FolderViewModel
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    folderId: String,
    initialIndex: Int,
    onNavigateBack: () -> Unit,
    viewModel: FolderViewModel = viewModel()
) {
    val allImages by viewModel.allImages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showUI by remember { mutableStateOf(true) }

    LaunchedEffect(folderId) {
        viewModel.loadFolder(folderId)
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, maxOf(0, allImages.size - 1)),
        pageCount = { allImages.size }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
            allImages.isEmpty() -> {
                Text(
                    text = "No images to display",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
            else -> {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val zoomState = rememberZoomState()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zoomable(zoomState)
                    ) {
                        AsyncImage(
                            model = allImages[page].uri,
                            contentDescription = allImages[page].name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        // Top Bar (Overlay)
        if (showUI && allImages.isNotEmpty()) {
            TopAppBar(
                title = {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${allImages.size}",
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            )
        }

        // Bottom Info (Overlay)
        if (showUI && allImages.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = allImages[pagerState.currentPage].name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}
