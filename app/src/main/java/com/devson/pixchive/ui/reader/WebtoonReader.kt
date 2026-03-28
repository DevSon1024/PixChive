package com.devson.pixchive.ui.reader

import androidx.compose.foundation.background
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.request.ImageRequest
import coil.size.Size
import com.devson.pixchive.data.ImageFile
import com.devson.pixchive.data.local.ImageEntity
import com.devson.pixchive.viewmodel.FolderViewModel
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

@Composable
fun WebtoonReader(
    folderId: String,
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Handle single taps on the empty spaces in the Box, if any
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onToggleUI() })
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
                    folderId = folderId,
                    page = page,
                    chapterImages = chapterImages,
                    isFlatView = isFlatView,
                    flatImageCache = flatImageCache,
                    viewModel = viewModel,
                    context = context,
                    onClick = { onToggleUI() }
                )
            }
        }
    }
}

@Composable
private fun WebtoonPageItem(
    folderId: String,
    page: Int,
    chapterImages: List<Any>,
    isFlatView: Boolean,
    flatImageCache: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, ImageEntity?>,
    viewModel: FolderViewModel,
    context: android.content.Context,
    onClick: () -> Unit
) {
    if (isFlatView && !flatImageCache.containsKey(page)) {
        LaunchedEffect(page) {
            flatImageCache[page] = viewModel.getFlatImageAt(page, folderId)
        }
    }

    val pageImage: Any? = if (isFlatView) {
        flatImageCache[page]
    } else {
        chapterImages.getOrNull(page)
    }

    val imageUri = when (pageImage) {
        is ImageEntity -> pageImage.uri
        is ImageFile   -> pageImage.uri.toString()
        else           -> null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .wrapContentHeight()
    ) {
        val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 5f))

        ZoomableAsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUri)
                .size(Size.ORIGINAL)
                .crossfade(false)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            state = rememberZoomableImageState(zoomableState),
            onClick = { onClick() },
            contentScale = ContentScale.FillWidth
        )
    }
}