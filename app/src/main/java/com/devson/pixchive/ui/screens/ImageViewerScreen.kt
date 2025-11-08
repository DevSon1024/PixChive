package com.devson.pixchive.ui.screens

import android.net.Uri
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

// Helper function to match URIs
private fun urisMatch(uri1: String, uri2: String): Boolean {
    return try {
        val parsed1 = Uri.parse(uri1)
        val parsed2 = Uri.parse(uri2)

        parsed1.scheme == parsed2.scheme &&
                parsed1.authority == parsed2.authority &&
                parsed1.path == parsed2.path
    } catch (e: Exception) {
        uri1 == uri2
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    folderId: String,
    chapterPath: String,  // NEW: Pass chapter path
    initialIndex: Int,
    onNavigateBack: () -> Unit,
    viewModel: FolderViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val chapters by viewModel.chapters.collectAsState()

    // Get ONLY the images from this specific chapter
    val chapterImages = remember(chapters, chapterPath) {
        chapters.find { chapter ->
            urisMatch(chapter.path, chapterPath)
        }?.images ?: emptyList()
    }

    var showUI by remember { mutableStateOf(true) }

    LaunchedEffect(folderId) {
        viewModel.loadFolder(folderId)
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, maxOf(0, chapterImages.size - 1)),
        pageCount = { chapterImages.size }
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
            chapterImages.isEmpty() -> {
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
                    val zoomState = rememberZoomState(maxScale = 5f)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zoomable(zoomState)
                    ) {
                        AsyncImage(
                            model = chapterImages[page].uri,
                            contentDescription = chapterImages[page].name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        // Top Bar
        if (showUI && chapterImages.isNotEmpty()) {
            TopAppBar(
                title = {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${chapterImages.size}",
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

        // Bottom Info
        if (showUI && chapterImages.isNotEmpty()) {
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
                        text = chapterImages[pagerState.currentPage].name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Pinch to zoom • Swipe to navigate",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}