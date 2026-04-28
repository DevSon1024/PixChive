package com.devson.pixchive.gallery.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.gallery.data.models.GalleryImage
import com.devson.pixchive.gallery.viewmodel.GalleryFolderViewModel
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ImageViewScreen(
    bucketId: String,
    initialIndex: Int,
    onNavigateBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: GalleryFolderViewModel = viewModel()
) {
    val images by viewModel.images.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showOverlays by remember { mutableStateOf(true) }
    var showInfoDialog by remember { mutableStateOf<GalleryImage?>(null) }

    // --- IMMERSIVE MODE LOGIC ---
    val context = LocalContext.current
    val view = LocalView.current
    val window = (context as Activity).window
    val insetsController = remember { WindowCompat.getInsetsController(window, view) }

    LaunchedEffect(showOverlays) {
        if (showOverlays) {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        } else {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Guarantee system bars are restored when leaving this screen
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(bucketId) {
        // If coming from the grid, it might already be cached in a shared VM setup, 
        // but calling it ensures we have data.
        if (images.isEmpty()) viewModel.loadImages(bucketId)
    }

    if (isLoading || images.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }

    // Set initial page from the grid click
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, images.lastIndex),
        pageCount = { images.size }
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { images[it].id }
        ) { page ->
            val zoomableState = rememberZoomableImageState()

            // Replace your ZoomableAsyncImage modifier with this:
            with(sharedTransitionScope) {
                ZoomableAsyncImage(
                    model = images[page].uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "image_${images[page].id}"), // <-- FIXED HERE
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                    state = zoomableState,
                    onClick = { showOverlays = !showOverlays }
                )
            }
        }

        // Overlay App Bar
        AnimatedVisibility(
            visible = showOverlays,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val currentImage = images[pagerState.currentPage]
            val dateString = remember(currentImage.dateModified) {
                SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
                    .format(Date(currentImage.dateModified * 1000L))
            }

            TopAppBar(
                title = {
                    Column {
                        Text("${pagerState.currentPage + 1} / ${images.size}", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(dateString, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = currentImage }) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            )
        }
    }

    showInfoDialog?.let { image ->
        AlertDialog(
            onDismissRequest = { showInfoDialog = null },
            title = { Text("Image Details") },
            text = {
                Column {
                    Text("File Path:", style = MaterialTheme.typography.labelMedium)
                    Text(image.realPath, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { showInfoDialog = null }) { Text("Close") } }
        )
    }
}