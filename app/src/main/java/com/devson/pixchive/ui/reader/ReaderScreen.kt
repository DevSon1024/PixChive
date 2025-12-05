package com.devson.pixchive.ui.reader

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.request.ImageRequest
import com.devson.pixchive.ui.reader.components.ReaderActionButton
import com.devson.pixchive.ui.reader.components.ReaderTopBar
import com.devson.pixchive.ui.reader.utils.urisMatch
import com.devson.pixchive.viewmodel.FolderViewModel
import kotlinx.coroutines.launch
// Telephoto Imports
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.ZoomSpec

@Composable
fun ReaderScreen(
    folderId: String,
    chapterPath: String,
    initialIndex: Int,
    onNavigateBack: () -> Unit,
    viewModel: FolderViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val isLoading by viewModel.isLoading.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val currentFolder by viewModel.currentFolder.collectAsState()

    val chapterImages = remember(chapters, chapterPath) {
        chapters.find { chapter ->
            urisMatch(chapter.path, chapterPath)
        }?.images ?: emptyList()
    }

    val chapterName = remember(chapterPath) {
        chapterPath.substringAfterLast("/").substringAfterLast(":")
    }

    var showUI by remember { mutableStateOf(true) }
    var showBottomOptions by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var readingMode by remember { mutableStateOf("fit") }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, maxOf(0, chapterImages.size - 1)),
        pageCount = { chapterImages.size }
    )

    val currentImage = if (chapterImages.isNotEmpty() && pagerState.currentPage < chapterImages.size) {
        chapterImages[pagerState.currentPage]
    } else null

    LaunchedEffect(showUI) {
        activity?.window?.let { window ->
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            if (showUI) {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(folderId) {
        viewModel.loadFolder(folderId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showUI = !showUI
                        if (!showUI) showBottomOptions = false
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
                    // FIX: Use ZoomSpec for maxZoomFactor
                    val zoomableState = rememberZoomableState(
                        zoomSpec = ZoomSpec(maxZoomFactor = 10f)
                    )

                    ZoomableAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(chapterImages[page].uri)
                            .build(),
                        contentDescription = chapterImages[page].name,
                        modifier = Modifier.fillMaxSize(),
                        state = rememberZoomableImageState(zoomableState),
                        contentScale = when (readingMode) {
                            "fill" -> ContentScale.Crop
                            "original" -> ContentScale.None
                            else -> ContentScale.Fit
                        }
                    )
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showUI && chapterImages.isNotEmpty(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            ReaderTopBar(
                chapterFolderName = chapterName,
                currentImageName = currentImage?.name?.substringBeforeLast('.') ?: "",
                showMoreMenu = showMoreMenu,
                currentImage = currentImage,
                onNavigateBack = onNavigateBack,
                onMoreMenuToggle = { showMoreMenu = it }
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showUI && chapterImages.isNotEmpty(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.85f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = { showBottomOptions = !showBottomOptions },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (showBottomOptions) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = if (showBottomOptions) "Hide Options" else "Show Options",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showBottomOptions,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ReaderActionButton(
                                icon = when (readingMode) {
                                    "fill" -> Icons.Default.CropSquare
                                    "original" -> Icons.Default.PhotoSizeSelectActual
                                    else -> Icons.Default.FitScreen
                                },
                                label = readingMode.uppercase(),
                                onClick = {
                                    readingMode = when (readingMode) {
                                        "fit" -> "fill"
                                        "fill" -> "original"
                                        else -> "fit"
                                    }
                                }
                            )
                            ReaderActionButton(
                                icon = Icons.Default.RotateRight,
                                label = "ROTATE",
                                onClick = { /* TODO */ }
                            )
                            ReaderActionButton(
                                icon = Icons.Default.Lock,
                                label = "LOCK",
                                onClick = { /* TODO */ }
                            )
                            ReaderActionButton(
                                icon = Icons.Default.Settings,
                                label = "SETTINGS",
                                onClick = { /* TODO */ }
                            )
                        }
                        Divider(color = Color.Gray.copy(alpha = 0.3f))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (pagerState.currentPage > 0) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage > 0,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (pagerState.currentPage > 0) Color.White else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "${pagerState.currentPage + 1}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(32.dp)
                    )

                    Slider(
                        value = pagerState.currentPage.toFloat(),
                        onValueChange = { newValue ->
                            scope.launch {
                                pagerState.scrollToPage(newValue.toInt())
                            }
                        },
                        valueRange = 0f..(chapterImages.size - 1).toFloat(),
                        steps = if (chapterImages.size > 2) chapterImages.size - 2 else 0,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.Gray
                        )
                    )

                    Text(
                        text = "${chapterImages.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(32.dp)
                    )

                    IconButton(
                        onClick = {
                            if (pagerState.currentPage < chapterImages.size - 1) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage < chapterImages.size - 1,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = if (pagerState.currentPage < chapterImages.size - 1) Color.White else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}