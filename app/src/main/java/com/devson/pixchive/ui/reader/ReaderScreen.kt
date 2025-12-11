package com.devson.pixchive.ui.reader

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.request.ImageRequest
import coil.size.Scale
import com.devson.pixchive.data.PreferencesManager
import com.devson.pixchive.ui.reader.components.ReaderActionButton
import com.devson.pixchive.ui.reader.components.ReaderTopBar
import com.devson.pixchive.ui.reader.utils.urisMatch
import com.devson.pixchive.viewmodel.FolderViewModel
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.ZoomSpec
import java.io.File

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
    val prefs = remember { PreferencesManager(context) }

    val isLoading by viewModel.isLoading.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val allImages by viewModel.allImages.collectAsState()
    val currentFolder by viewModel.currentFolder.collectAsState()
    val favorites by prefs.favoritesFlow.collectAsState(initial = emptySet())

    val chapterImages = remember(chapters, allImages, chapterPath) {
        if (chapterPath == "flat_view") allImages
        else chapters.find { urisMatch(it.path, chapterPath) }?.images ?: emptyList()
    }

    val chapterName = remember(chapterPath, currentFolder) {
        if (chapterPath == "flat_view") currentFolder?.name ?: "Flat View"
        else chapterPath.substringAfterLast("/").substringAfterLast(":")
    }

    var showUI by remember { mutableStateOf(true) }
    var showBottomOptions by remember { mutableStateOf(false) }
    var readingMode by remember { mutableStateOf("fit") }

    val rotationStates = remember { mutableStateMapOf<Int, Float>() }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, maxOf(0, chapterImages.size - 1)),
        pageCount = { chapterImages.size }
    )

    // SAFE ACCESS: Prevents crash if list shrinks while viewing
    val currentImage = if (chapterImages.isNotEmpty() && pagerState.currentPage < chapterImages.size) {
        chapterImages[pagerState.currentPage]
    } else null

    val isFavorite = currentImage?.uri.toString() in favorites

    LaunchedEffect(showUI) {
        activity?.window?.let { window ->
            val insets = WindowCompat.getInsetsController(window, view)
            insets.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (showUI) insets.show(WindowInsetsCompat.Type.systemBars())
            else insets.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(folderId) { viewModel.loadFolder(folderId) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        if (isLoading && chapterImages.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
        } else if (chapterImages.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                key(page) {
                    val rotation = rotationStates[page] ?: 0f

                    Box(modifier = Modifier.fillMaxSize()) {
                        val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 5f))
                        ZoomableAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(chapterImages.getOrNull(page)?.uri)
                                .size(2048)
                                .scale(Scale.FIT)
                                .crossfade(false)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationZ = rotation
                                    clip = false
                                },
                            state = rememberZoomableImageState(zoomableState),
                            onClick = {
                                showUI = !showUI
                                if (!showUI) showBottomOptions = false
                            },
                            contentScale = when (readingMode) {
                                "fill" -> ContentScale.Crop
                                "original" -> ContentScale.None
                                else -> ContentScale.Fit
                            }
                        )
                    }
                }
            }
        } else if (!isLoading) {
            Text("No images", Modifier.align(Alignment.Center), color = Color.White)
        }

        // --- UI Overlay ---

        AnimatedVisibility(
            visible = showUI,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            ReaderTopBar(
                chapterFolderName = chapterName,
                currentImageName = currentImage?.name ?: "",
                showMoreMenu = false,
                currentImage = currentImage,
                onNavigateBack = onNavigateBack,
                onMoreMenuToggle = {},
                isFavorite = isFavorite,
                onToggleFavorite = {
                    currentImage?.let { scope.launch { prefs.toggleFavorite(it.uri.toString()) } }
                }
            )
        }

        // Floating Arrow (Separate from bottom bar)
        AnimatedVisibility(
            visible = showUI,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (showBottomOptions) 140.dp else 80.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { showBottomOptions = !showBottomOptions },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(
                    imageVector = if (showBottomOptions) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = "Options"
                )
            }
        }

        AnimatedVisibility(
            visible = showUI,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                AnimatedVisibility(
                    visible = showBottomOptions,
                    enter = androidx.compose.animation.expandVertically(),
                    exit = androidx.compose.animation.shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ReaderActionButton(
                            icon = Icons.Default.RotateRight,
                            label = "ROTATE",
                            onClick = {
                                val currentRot = rotationStates[pagerState.currentPage] ?: 0f
                                rotationStates[pagerState.currentPage] = currentRot + 90f
                            }
                        )
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
                            icon = Icons.Default.Share,
                            label = "SHARE",
                            onClick = {
                                currentImage?.let { image ->
                                    try {
                                        val file = File(image.path)
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "image/*"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error sharing: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = pagerState.currentPage.toFloat(),
                        onValueChange = { scope.launch { pagerState.scrollToPage(it.toInt()) } },
                        valueRange = 0f..maxOf(0f, (chapterImages.size - 1).toFloat()),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.Gray
                        )
                    )
                    Text(
                        text = "${chapterImages.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}