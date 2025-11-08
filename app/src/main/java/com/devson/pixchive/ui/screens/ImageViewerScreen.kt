package com.devson.pixchive.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.viewmodel.FolderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    chapterPath: String,
    initialIndex: Int,
    onNavigateBack: () -> Unit,
    viewModel: FolderViewModel = viewModel()
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val chapters by viewModel.chapters.collectAsState()

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
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showUI = !showUI
                    }
                )
            }
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
                    val zoomState = rememberZoomState(maxScale = 10f)

                    // Load ORIGINAL bitmap directly from URI
                    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                    var isLoadingImage by remember { mutableStateOf(true) }

                    LaunchedEffect(chapterImages[page].uri) {
                        isLoadingImage = true
                        bitmap = withContext(Dispatchers.IO) {
                            try {
                                // Load ORIGINAL image with NO scaling or compression
                                context.contentResolver.openInputStream(chapterImages[page].uri)?.use { inputStream ->
                                    val options = BitmapFactory.Options().apply {
                                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                                        inScaled = false  // NO scaling
                                        inDither = false  // NO dithering
                                        inPreferQualityOverSpeed = true  // Best quality
                                    }
                                    BitmapFactory.decodeStream(inputStream, null, options)
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }
                        isLoadingImage = false
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zoomable(zoomState)
                    ) {
                        when {
                            isLoadingImage -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                    color = Color.White
                                )
                            }
                            bitmap != null -> {
                                Image(
                                    painter = BitmapPainter(bitmap!!.asImageBitmap()),
                                    contentDescription = chapterImages[page].name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            else -> {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "❌",
                                        style = MaterialTheme.typography.displayMedium
                                    )
                                    Text(
                                        text = "Failed to load image",
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Top Bar
        androidx.compose.animation.AnimatedVisibility(
            visible = showUI && chapterImages.isNotEmpty(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
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
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            )
        }

        // Bottom Info
        androidx.compose.animation.AnimatedVisibility(
            visible = showUI && chapterImages.isNotEmpty(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = chapterImages[pagerState.currentPage].name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Original Quality • Tap to toggle UI • Pinch to zoom • Swipe to navigate",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}