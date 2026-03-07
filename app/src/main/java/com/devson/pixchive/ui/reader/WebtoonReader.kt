package com.devson.pixchive.ui.reader

import androidx.compose.foundation.background
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.devson.pixchive.data.ImageFile
import com.devson.pixchive.data.local.ImageEntity
import com.devson.pixchive.viewmodel.FolderViewModel
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

/**
 * Webtoon-style vertical strip reader.
 *
 * Images are stacked seamlessly in a [LazyColumn], each scaled to fill the full
 * width so the comic flows as a continuous vertical strip - perfect for webtoon
 * and manhwa formats.
 *
 * Regular [AsyncImage] (not ZoomableAsyncImage) is used deliberately: Telephoto's
 * zoomable widget intercepts vertical drag gestures and prevents the [LazyColumn]
 * from scrolling past the first couple of items.
 *
 * @param chapterImages  Non-empty list of [ImageEntity] for chapter mode; empty for flat-view.
 * @param isFlatView     Whether the reader is in flat (all-images) mode.
 * @param pageCount      Total number of pages.
 * @param flatImageCache Shared cache map for on-demand DB image loading in flat mode.
 * @param listState      Shared [LazyListState] so the caller can track scrolled position.
 * @param onPageChanged  Called whenever the visible item index changes.
 * @param viewModel      ViewModel for on-demand flat-image loading.
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

    // Keep caller's page counter in sync with scroll position
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { onPageChanged(it) }
    }

    val zoomableState = rememberZoomableState(
        zoomSpec = ZoomSpec(maxZoomFactor = 4f)
    )

    // The LazyColumn is wrapped in a Box with Modifier.zoomable(). This allows the 
    // entire webtoon strip to be pinch-zoomed. Telephoto handles nested scrolling, 
    // so standard 1-finger vertical swipes still scroll the LazyColumn when scale is 1x.
    // It also natively gives us simple tap detection (onClick) for the UI overlay.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .zoomable(
                state = zoomableState,
                onClick = { onToggleUI() }
            )
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
    // For flat view: load image on-demand if not yet cached
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

    // Each image fills the screen width; height is derived from the image's natural
    // aspect ratio (ContentScale.FillWidth). heightIn(min = 200.dp) prevents the
    // item collapsing to 0 height while loading, which would break scroll measurement.
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