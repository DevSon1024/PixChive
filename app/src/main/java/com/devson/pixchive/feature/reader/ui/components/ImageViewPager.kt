package com.devson.pixchive.feature.reader.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import com.devson.pixchive.core.data.ImageFile

@Composable
fun ImageViewPager(
    images: List<ImageFile>,
    initialPage: Int,
    readingMode: String = "fit",
    mangaMode: Boolean = false,
    isLoading: Boolean = false,
    onPageChanged: (Int) -> Unit = {}
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, maxOf(0, images.size - 1)),
        pageCount = { images.size }
    )

    var currentZoomScale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            images.isEmpty() -> {
                Text(
                    text = "No images to display",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
            else -> {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = mangaMode,
                    userScrollEnabled = currentZoomScale <= 1.05f
                ) { page ->
                    var imageSize by remember { mutableStateOf(IntSize.Zero) }

                    // State for scaling and translating
                    var scale by remember { mutableFloatStateOf(1f) }
                    var offsetX by remember { mutableFloatStateOf(0f) }
                    var offsetY by remember { mutableFloatStateOf(0f) }

                    val maxScale = 3f
                    val minScale = 1f

                    // Update parent zoom state when this page is active
                    LaunchedEffect(scale, pagerState.currentPage == page) {
                        if (pagerState.currentPage == page) {
                            currentZoomScale = scale
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { imageSize = it }
                            .pointerInput(page) {
                                detectTapGestures(
                                    onDoubleTap = { tapOffset ->
                                        if (scale > 1f) {
                                            // Reset to original size
                                            scale = 1f
                                            offsetX = 0f
                                            offsetY = 0f
                                        } else {
                                            // Zoom in to exactly where the user tapped
                                            scale = 2.5f
                                            val centerX = size.width / 2f
                                            val centerY = size.height / 2f
                                            val newOffsetX = (centerX - tapOffset.x) * (scale - 1f)
                                            val newOffsetY = (centerY - tapOffset.y) * (scale - 1f)

                                            val maxX = (size.width * (scale - 1)) / 2f
                                            val maxY = (size.height * (scale - 1)) / 2f

                                            offsetX = newOffsetX.coerceIn(-maxX, maxX)
                                            offsetY = newOffsetY.coerceIn(-maxY, maxY)
                                        }
                                    }
                                )
                            }
                            .pointerInput(page) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    val oldScale = scale
                                    val newScale = (scale * zoom).coerceIn(minScale, maxScale)

                                    val maxX = (size.width * (newScale - 1)) / 2f
                                    val maxY = (size.height * (newScale - 1)) / 2f

                                    if (newScale > 1f) {
                                        val x0 = centroid.x - size.width / 2f
                                        val y0 = centroid.y - size.height / 2f

                                        val dx = x0 - (x0 - offsetX) * (newScale / oldScale)
                                        val dy = y0 - (y0 - offsetY) * (newScale / oldScale)

                                        offsetX = (dx + pan.x).coerceIn(-maxX, maxX)
                                        offsetY = (dy + pan.y).coerceIn(-maxY, maxY)
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }

                                    scale = newScale
                                }
                            }
                    ) {
                        AsyncImage(
                            model = images[page].uri,
                            contentDescription = images[page].name,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                ),
                            contentScale = when (readingMode) {
                                "fill" -> ContentScale.Crop
                                "original" -> ContentScale.None
                                else -> ContentScale.Fit
                            }
                        )
                    }
                }
            }
        }
    }
}