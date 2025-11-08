package com.devson.pixchive.ui.reader.components

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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.devson.pixchive.data.ImageFile
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@Composable
fun ImageViewPager(
    images: List<ImageFile>,
    initialPage: Int,
    readingMode: String,
    isLoading: Boolean,
    onPageChanged: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                    val zoomState = rememberZoomState(maxScale = 10f)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zoomable(zoomState)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(images[page].uri)
                                .size(Size.ORIGINAL)
                                .allowHardware(true)
                                .build(),
                            contentDescription = images[page].name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = when (readingMode) {
                                "fill" -> ContentScale.Crop
                                "original" -> ContentScale.None
                                else -> ContentScale.Fit
                            },
                            filterQuality = FilterQuality.None
                        )
                    }
                }
            }
        }
    }
}