package com.devson.pixchive.ui.reader.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import coil.compose.AsyncImage
import com.devson.pixchive.data.ImageFile
import kotlinx.coroutines.launch

@Composable
fun ImageViewPager(
    images: List<ImageFile>,
    initialPage: Int,
    readingMode: String,
    isLoading: Boolean,
    onPageChanged: (Int) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, maxOf(0, images.size - 1)),
        pageCount = { images.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
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
                    userScrollEnabled = true // Allows pager swipe when not horizontally panning the zoomed image
                ) { page ->
                    
                    var imageSize by remember { mutableStateOf(IntSize.Zero) }
                    
                    // State for scaling and translating
                    var scale by remember { mutableStateOf(1f) }
                    var offsetX by remember { mutableStateOf(0f) }
                    var offsetY by remember { mutableStateOf(0f) }
                    
                    val coroutineScope = rememberCoroutineScope()
                    val maxScale = 2.5f
                    val minScale = 1f

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
                                            // Calculate the offset so that the tapped point remains in the same position on screen.
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
                                        // Adjust offset mathematically so the image zooms based on centroid
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
