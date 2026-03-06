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
import com.devson.pixchive.data.ImageFile
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

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
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val zoomableState = rememberZoomableState(
                        zoomSpec = me.saket.telephoto.zoomable.ZoomSpec(maxZoomFactor = 10f)
                    )

                    ZoomableAsyncImage(
                        model = images[page].uri,
                        contentDescription = images[page].name,
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
    }
}
