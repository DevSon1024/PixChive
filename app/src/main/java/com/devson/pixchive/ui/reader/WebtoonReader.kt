package com.devson.pixchive.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devson.pixchive.data.ImageFile
import com.devson.pixchive.data.local.ImageEntity
import com.devson.pixchive.viewmodel.FolderViewModel
import kotlin.math.abs

/**
 * Webtoon-style vertical strip reader.
 *
 * Images are stacked seamlessly in a [LazyColumn], each scaled to fill the full
 * width so the comic flows as a continuous vertical strip.
 *
 * Zoom uses a custom [pointerInput] that only activates on 2+ finger touches,
 * ensuring single-finger scroll is always passed cleanly to [LazyColumn] without
 * any mistouch or interception issues.
 */
@Composable
fun WebtoonReader(
    chapterImages: List<Any>,
    isFlatView: Boolean,
    pageCount: Int,
    flatImageCache: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, ImageEntity?>,
    listState: LazyListState = rememberLazyListState(),
    onPageChanged: (Int) -> Unit = {},
    onToggleUI: () -> Unit = {},
    viewModel: FolderViewModel
) {
    val context = LocalContext.current

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { onPageChanged(it) }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Apply zoom transform — never modified by single-finger events
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
            // Custom 2-finger pinch detector using PointerEventPass.Initial so we
            // see events BEFORE LazyColumn, but we only act if pointer count >= 2.
            // Single-finger events are never consumed here, so LazyColumn scrolls freely.
            .pointerInput(Unit) {
                awaitEachGesture {
                    // Wait for the first finger down without consuming it
                    awaitFirstDown(requireUnconsumed = false)

                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val activePointers = event.changes.filter { it.pressed }

                        if (activePointers.size >= 2) {
                            // --- Two-finger gesture: handle zoom + pan ---
                            val zoomChange = event.calculateZoom()
                            val newScale = (scale * zoomChange).coerceIn(1f, 4f)

                            val centroid = event.calculateCentroid()
                            val panChange = activePointers
                                .map { it.position - it.previousPosition }
                                .fold(Offset.Zero) { acc, o -> acc + o } / activePointers.size.toFloat()

                            val newOffset = if (newScale > 1f) {
                                val maxTranslation = size.width * (newScale - 1f) / 2f
                                val maxTranslationY = size.height * (newScale - 1f) / 2f
                                Offset(
                                    x = (offset.x + panChange.x).coerceIn(-maxTranslation, maxTranslation),
                                    y = (offset.y + panChange.y).coerceIn(-maxTranslationY, maxTranslationY)
                                )
                            } else {
                                Offset.Zero
                            }

                            scale = newScale
                            offset = newOffset

                            // Consume all pointer changes so LazyColumn doesn't scroll during pinch
                            event.changes.forEach { it.consume() }
                        } else if (scale > 1f && activePointers.size == 1) {
                            // --- Single finger while zoomed: allow panning ---
                            val change = activePointers.first()
                            if (change.positionChanged()) {
                                val delta = change.position - change.previousPosition
                                val maxTranslation = size.width * (scale - 1f) / 2f
                                val maxTranslationY = size.height * (scale - 1f) / 2f
                                offset = Offset(
                                    x = (offset.x + delta.x).coerceIn(-maxTranslation, maxTranslation),
                                    y = (offset.y + delta.y).coerceIn(-maxTranslationY, maxTranslationY)
                                )
                                change.consume()
                            }
                        }
                        // Single finger at scale==1f: do nothing — LazyColumn scrolls freely
                    } while (event.changes.any { it.pressed })

                    // On lift, if returned to 1x zoom snap offset to zero
                    if (scale <= 1f) offset = Offset.Zero
                }
            }
            // Tap to toggle UI overlay — low priority so zoom gestures take precedence
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleUI() }
                )
            }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = pageCount,
                key = { page -> page }
            ) { page ->
                WebtoonPageItem(
                    page = page,
                    chapterImages = chapterImages,
                    isFlatView = isFlatView,
                    flatImageCache = flatImageCache,
                    viewModel = viewModel,
                    context = context
                )
            }
        }
    }
}

@Composable
private fun WebtoonPageItem(
    page: Int,
    chapterImages: List<Any>,
    isFlatView: Boolean,
    flatImageCache: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, ImageEntity?>,
    viewModel: FolderViewModel,
    context: android.content.Context
) {
    if (isFlatView && !flatImageCache.containsKey(page)) {
        LaunchedEffect(page) {
            flatImageCache[page] = viewModel.getFlatImageAt(page)
        }
    }

    val pageImage: Any? = if (isFlatView) {
        flatImageCache[page]
    } else {
        chapterImages.getOrNull(page)
    }

    val imageUri = when (pageImage) {
        is ImageEntity -> pageImage.uri
        is ImageFile -> pageImage.uri.toString()
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .wrapContentHeight()
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUri)
                .crossfade(100)
                .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                .allowHardware(false)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentScale = ContentScale.FillWidth
        )
    }
}