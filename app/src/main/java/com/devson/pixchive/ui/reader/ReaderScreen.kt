package com.devson.pixchive.ui.reader

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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

    val currentImage = if (chapterImages.isNotEmpty() && pagerState.currentPage < chapterImages.size) {
        chapterImages[pagerState.currentPage]
    } else null

    val isFavorite = currentImage?.uri.toString() in favorites

    // Animation values
    val uiAlpha by animateFloatAsState(
        targetValue = if (showUI) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "uiAlpha"
    )

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
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isLoading && chapterImages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1A1A1A),
                                Color.Black
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
            }
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
                                .crossfade(true)
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0D0D0D),
                                Color.Black
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        "No images found",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // --- Enhanced UI Overlay ---

        // Top gradient backdrop
        AnimatedVisibility(
            visible = showUI,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        AnimatedVisibility(
            visible = showUI,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it / 2 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 2 },
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

        // --- Enhanced Bottom Controls ---
        AnimatedVisibility(
            visible = showUI,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
            ) {
                // Floating toggle button with pulse effect
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (showBottomOptions) 1f else 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                Surface(
                    onClick = { showBottomOptions = !showBottomOptions },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(52.dp)
                        .scale(if (showBottomOptions) 1f else pulseScale)
                        .padding(bottom = 12.dp),
                    tonalElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (showBottomOptions) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "Toggle Options",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .size(28.dp)
                                .graphicsLayer {
                                    rotationZ = if (showBottomOptions) 0f else 0f
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Enhanced floating panel with glassmorphism
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    tonalElevation = 12.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                    ) {
                        // Animated options row
                        AnimatedVisibility(
                            visible = showBottomOptions,
                            enter = expandVertically(tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(300)),
                            exit = shrinkVertically(tween(200, easing = FastOutSlowInEasing)) + fadeOut(tween(200))
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    EnhancedReaderActionButton(
                                        icon = Icons.Default.RotateRight,
                                        label = "Rotate",
                                        onClick = {
                                            val currentRot = rotationStates[pagerState.currentPage] ?: 0f
                                            rotationStates[pagerState.currentPage] = currentRot + 90f
                                        }
                                    )
                                    EnhancedReaderActionButton(
                                        icon = when (readingMode) {
                                            "fill" -> Icons.Default.CropSquare
                                            "original" -> Icons.Default.PhotoSizeSelectActual
                                            else -> Icons.Default.FitScreen
                                        },
                                        label = when (readingMode) {
                                            "fill" -> "Fill"
                                            "original" -> "Original"
                                            else -> "Fit"
                                        },
                                        onClick = {
                                            readingMode = when (readingMode) {
                                                "fit" -> "fill"
                                                "fill" -> "original"
                                                else -> "fit"
                                            }
                                        }
                                    )
                                    EnhancedReaderActionButton(
                                        icon = Icons.Default.Share,
                                        label = "Share",
                                        onClick = {
                                            currentImage?.let { image ->
                                                try {
                                                    val file = File(image.path)
                                                    val uri = FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.provider",
                                                        file
                                                    )
                                                    val shareIntent = android.content.Intent(
                                                        android.content.Intent.ACTION_SEND
                                                    ).apply {
                                                        type = "image/*"
                                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(
                                                        android.content.Intent.createChooser(
                                                            shareIntent,
                                                            "Share"
                                                        )
                                                    )
                                                } catch (e: Exception) {
                                                    Toast.makeText(
                                                        context,
                                                        "Error sharing",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    )
                                }

                                // Divider
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            }
                        }

                        // Enhanced slider row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Text(
                                    text = "${pagerState.currentPage + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            Slider(
                                value = pagerState.currentPage.toFloat(),
                                onValueChange = {
                                    scope.launch {
                                        pagerState.scrollToPage(it.toInt())
                                    }
                                },
                                valueRange = 0f..maxOf(0f, (chapterImages.size - 1).toFloat()),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                Text(
                                    text = "${chapterImages.size}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedReaderActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )

    Surface(
        onClick = {
            isPressed = true
            onClick()
        },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        modifier = Modifier
            .scale(scale)
            .defaultMinSize(minWidth = 80.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}