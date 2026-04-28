package com.devson.pixchive.gallery.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.devson.pixchive.gallery.data.models.GalleryImage
import com.devson.pixchive.gallery.viewmodel.GalleryViewerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GalleryViewerScreen(
    bucketId: String,
    onNavigateBack: () -> Unit,
    viewModel: GalleryViewerViewModel = viewModel()
) {
    val images by viewModel.images.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showOverlays by remember { mutableStateOf(true) }
    var showInfoDialog by remember { mutableStateOf<GalleryImage?>(null) }

    LaunchedEffect(bucketId) {
        viewModel.loadImages(bucketId)
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    if (images.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("No images found.", color = Color.White)
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { images.size })

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showOverlays = !showOverlays }
        ) { page ->
            ZoomableImage(image = images[page])
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
                    containerColor = Color.Black.copy(alpha = 0.6f)
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
            confirmButton = {
                TextButton(onClick = { showInfoDialog = null }) { Text("Close") }
            }
        )
    }
}

@Composable
fun ZoomableImage(image: GalleryImage) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    val maxOffset = (scale - 1) * 1000f // Rough boundary
                    offsetX = (offsetX + pan.x).coerceIn(-maxOffset, maxOffset)
                    offsetY = (offsetY + pan.y).coerceIn(-maxOffset, maxOffset)
                }
            }
    ) {
        AsyncImage(
            model = image.uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = if (scale > 1f) offsetX else 0f,
                    translationY = if (scale > 1f) offsetY else 0f
                )
        )
    }
}