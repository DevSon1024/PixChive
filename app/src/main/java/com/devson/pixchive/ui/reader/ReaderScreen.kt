package com.devson.pixchive.ui.reader

import android.app.Activity
import android.media.AudioManager
import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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
import com.devson.pixchive.data.local.ImageEntity
import kotlinx.coroutines.delay
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
    val currentFolder by viewModel.currentFolder.collectAsState()
    val favorites by prefs.favoritesFlow.collectAsState(initial = emptySet())
    val readerScrollMode by viewModel.readerScrollMode.collectAsState()
    val mangaMode by viewModel.mangaMode.collectAsState()

    // --- FLAT VIEW: use true DB total instead of paging snapshot ---
    val flatImageCount by viewModel.flatImageCount.collectAsState()
    val isFlatView = chapterPath == "flat_view"

    // For chapter view, image list comes from chapters as before
    val chapterImages = remember(chapters, chapterPath) {
        if (isFlatView) emptyList()
        else chapters.find { urisMatch(it.path, chapterPath) }?.images ?: emptyList()
    }

    val pageCount = if (isFlatView) flatImageCount else chapterImages.size

    val chapterName = remember(chapterPath, currentFolder) {
        if (isFlatView) currentFolder?.name ?: "Flat View"
        else chapterPath.substringAfterLast("/").substringAfterLast(":")
    }

    var showUI by remember { mutableStateOf(true) }
    var showBottomOptions by remember { mutableStateOf(false) }
    var readingMode by remember { mutableStateOf("fit") }

    val rotationStates = remember { mutableStateMapOf<Int, Float>() }

    // ── AUTO-RESUME: resolved initial page ─────────────────────────────────────
    // IMPORTANT: do NOT coerce against pageCount here - for flat view, flatImageCount
    // is 0 at first composition (the StateFlow hasn't emitted yet), so coerceIn(0,0)
    // would silently clamp ANY clicked index to 0. Pass initialIndex raw instead;
    // the pageCount lambda in rememberPagerState keeps the pager bounded live.
    var resolvedInitialPage by remember { mutableStateOf(initialIndex) }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,          // raw - never clamp against stale pageCount
        pageCount = { pageCount }
    )

    // Once the real page count loads AND we know the saved/target page,
    // scroll the pager to it imperatively (only on first arrival).
    var didScrollToInitial by remember { mutableStateOf(false) }
    LaunchedEffect(folderId, chapterPath) {
        val saved = viewModel.getReadProgress(chapterPath)
        // Only use saved progress if the user didn't deep-link to a specific page
        resolvedInitialPage = if (initialIndex == 0 && saved > 0) saved else initialIndex
    }
    LaunchedEffect(pageCount, resolvedInitialPage) {
        if (!didScrollToInitial && pageCount > 0 && resolvedInitialPage > 0) {
            val target = resolvedInitialPage.coerceIn(0, pageCount - 1)
            pagerState.scrollToPage(target)
            didScrollToInitial = true
        } else if (pageCount > 0 && resolvedInitialPage == 0) {
            didScrollToInitial = true   // page 0 needs no scrolling
        }
    }

    // Webtoon scroll state (shared list state so we can read current position)
    val webtoonListState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex  // raw - same reason as pagerState above
    )

    // Track current page for both modes
    var webtoonCurrentPage by remember { mutableStateOf(resolvedInitialPage) }

    // Sync scroll position when switching between reading modes
    LaunchedEffect(readerScrollMode) {
        when (readerScrollMode) {
            "webtoon" -> {
                val page = pagerState.currentPage
                webtoonCurrentPage = page
                if (page > 0) webtoonListState.scrollToItem(page)
            }
            else -> { // "pager"
                pagerState.scrollToPage(webtoonCurrentPage)
            }
        }
    }

    val currentPage = if (readerScrollMode == "webtoon") webtoonCurrentPage else pagerState.currentPage

    // Local state for slider drag - decoupled from currentPage to avoid feedback loop.
    // While the user is dragging, we show the dragged value; on release we commit the scroll.
    var sliderDragging by remember { mutableStateOf(false) }
    var sliderDragValue by remember { mutableStateOf(0f) }

    // Cache for on-demand loaded flat-view images (page index -> ImageEntity)
    val flatImageCache = remember { mutableStateMapOf<Int, ImageEntity?>() }

    // Current image resolved for either mode
    val currentImage: Any? = if (isFlatView) {
        flatImageCache[currentPage]
    } else {
        if (chapterImages.isNotEmpty() && currentPage < chapterImages.size)
            chapterImages[currentPage]
        else null
    }

    val isFavorite = when (currentImage) {
        is ImageEntity -> currentImage.uri in favorites
        is com.devson.pixchive.data.ImageFile -> currentImage.uri.toString() in favorites
        else -> false
    }

    // ── TRUE IMMERSIVE MODE ─────────────────────────────────────────────────────
    // System bars are ALWAYS hidden for the full reader session.
    // The center-tap only toggles the Compose UI overlay, NOT the system bars.
    DisposableEffect(Unit) {
        val window = activity?.window
        val insets = window?.let { WindowCompat.getInsetsController(it, view) }

        // Bars auto-show transiently on edge-swipe, then re-hide themselves
        insets?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insets?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            // Restore bars when leaving the reader
            insets?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    //  VOLUME BUTTON NAVIGATION 
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager }
    val focusRequester = remember { FocusRequester() }

    // Suppress system volume overlay while in reader
    DisposableEffect(Unit) {
        @Suppress("DEPRECATION")
        audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        onDispose {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // AUTO-SAVE READ PROGRESS
    // Pager mode: save on page settle
    LaunchedEffect(pagerState.currentPage, readerScrollMode) {
        if (readerScrollMode != "webtoon" && pageCount > 0) {
            delay(500)
            viewModel.saveReadProgress(chapterPath, pagerState.currentPage)
        }
    }
    // Webtoon mode: save on scroll stop
    LaunchedEffect(webtoonCurrentPage, readerScrollMode) {
        if (readerScrollMode == "webtoon" && pageCount > 0) {
            delay(500)
            viewModel.saveReadProgress(chapterPath, webtoonCurrentPage)
        }
    }

    LaunchedEffect(folderId) { viewModel.loadFolder(folderId) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_VOLUME_DOWN -> {
                            scope.launch {
                                if (readerScrollMode == "webtoon") {
                                    val next = (webtoonCurrentPage + 1).coerceAtMost(pageCount - 1)
                                    webtoonListState.animateScrollToItem(next)
                                } else {
                                    val next = (pagerState.currentPage + 1).coerceAtMost(pageCount - 1)
                                    pagerState.animateScrollToPage(next)
                                }
                            }
                            true
                        }
                        KeyEvent.KEYCODE_VOLUME_UP -> {
                            scope.launch {
                                if (readerScrollMode == "webtoon") {
                                    val prev = (webtoonCurrentPage - 1).coerceAtLeast(0)
                                    webtoonListState.animateScrollToItem(prev)
                                } else {
                                    val prev = (pagerState.currentPage - 1).coerceAtLeast(0)
                                    pagerState.animateScrollToPage(prev)
                                }
                            }
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startY = down.position.y
                    var currentY = startY
                    var isConsumedByChild = false
                    
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val change = event.changes.firstOrNull()
                        if (change != null) {
                            currentY = change.position.y
                            if (change.isConsumed) {
                                isConsumedByChild = true
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    val deltaY = startY - currentY
                    if (!isConsumedByChild && deltaY > 100f) {
                        showUI = true
                        showBottomOptions = true
                    }
                }
            }
    ) {
        if (isLoading && pageCount == 0) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
        } else if (pageCount > 0) {

            // ── WEBTOON MODE ───────────────────────────────────────────────────
            if (readerScrollMode == "webtoon") {
                WebtoonReader(
                    chapterImages = chapterImages,
                    isFlatView = isFlatView,
                    pageCount = pageCount,
                    flatImageCache = flatImageCache,
                    listState = webtoonListState,
                    onPageChanged = { webtoonCurrentPage = it },
                    onToggleUI = {
                        showUI = !showUI
                        if (!showUI) showBottomOptions = false
                    },
                    viewModel = viewModel
                )
            } else {
                // ── HORIZONTAL PAGER MODE (with optional Manga RTL) ────────────
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    reverseLayout = mangaMode
                ) { page ->
                    key(page) {
                        // For flat view: load image on-demand if not cached yet
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

                        val rotation = rotationStates[page] ?: 0f
                        Box(modifier = Modifier.fillMaxSize()) {
                            val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 5f))
                            ZoomableAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(
                                        when (pageImage) {
                                            is ImageEntity -> pageImage.uri
                                            is com.devson.pixchive.data.ImageFile -> pageImage.uri
                                            else -> null
                                        }
                                    )
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
                currentImageName = when (currentImage) {
                    is ImageEntity -> currentImage.name
                    is com.devson.pixchive.data.ImageFile -> currentImage.name
                    else -> ""
                },
                showMoreMenu = false,
                currentImage = currentImage as? com.devson.pixchive.data.ImageFile,
                currentImageEntity = currentImage as? ImageEntity,
                onNavigateBack = onNavigateBack,
                onMoreMenuToggle = {},
                isFavorite = isFavorite,
                onToggleFavorite = {
                    currentImage?.let {
                        scope.launch {
                            if (it is ImageEntity) prefs.toggleFavorite(it.uri)
                            else if (it is com.devson.pixchive.data.ImageFile) prefs.toggleFavorite(it.uri.toString())
                        }
                    }
                }
            )
        }

        // --- Bottom Floating Controls ---
        AnimatedVisibility(
            visible = showUI,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -10f) {
                                showBottomOptions = true
                            } else if (dragAmount > 10f) {
                                showBottomOptions = false
                            }
                        }
                    }
            ) {
                // Floating Arrow (Separate Pill)
                Surface(
                    onClick = { showBottomOptions = !showBottomOptions },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                    modifier = Modifier.size(40.dp).padding(bottom = 8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (showBottomOptions) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Floating "Dynamic" Panel
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Options Row
                        AnimatedVisibility(
                            visible = showBottomOptions,
                            enter = androidx.compose.animation.expandVertically(),
                            exit = androidx.compose.animation.shrinkVertically()
                        ) {
                            Column {
                                // Row 1: Core actions
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    ReaderActionButton(
                                        icon = Icons.AutoMirrored.Filled.RotateRight,
                                        label = "ROTATE",
                                        onClick = {
                                            val currentRot = rotationStates[currentPage] ?: 0f
                                            rotationStates[currentPage] = currentRot + 90f
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
                                                    val path = when (image) {
                                                        is ImageEntity -> image.path
                                                        is com.devson.pixchive.data.ImageFile -> image.path
                                                        else -> return@let
                                                    }
                                                    val file = File(path)
                                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                        type = "image/*"
                                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share"))
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Error sharing", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    )
                                }

                                // Row 2: Reading mode toggles (Webtoon + Manga)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    // Webtoon / Pager toggle
                                    ReaderActionButton(
                                        icon = if (readerScrollMode == "webtoon")
                                            Icons.Default.ViewDay
                                        else
                                            Icons.Default.SwapVert,
                                        label = if (readerScrollMode == "webtoon") "PAGER" else "WEBTOON",
                                        isActive = readerScrollMode == "webtoon",
                                        onClick = {
                                            val newMode = if (readerScrollMode == "webtoon") "pager" else "webtoon"
                                            viewModel.setReaderScrollMode(newMode)
                                        }
                                    )
                                    // Manga RTL toggle
                                    ReaderActionButton(
                                        icon = Icons.AutoMirrored.Filled.CompareArrows,
                                        label = if (mangaMode) "LTR" else "MANGA",
                                        isActive = mangaMode,
                                        onClick = { viewModel.setMangaMode(!mangaMode) }
                                    )
                                }
                            }
                        }

                        // Slider Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${currentPage + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Slider(
                                value = if (sliderDragging) sliderDragValue else currentPage.toFloat(),
                                onValueChange = { target ->
                                    sliderDragging = true
                                    sliderDragValue = target
                                },
                                onValueChangeFinished = {
                                    sliderDragging = false
                                    val target = sliderDragValue.toInt()
                                    scope.launch {
                                        if (readerScrollMode == "webtoon") {
                                            webtoonListState.scrollToItem(target)
                                        } else {
                                            pagerState.scrollToPage(target)
                                        }
                                    }
                                },
                                valueRange = 0f..maxOf(0f, (pageCount - 1).toFloat()),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                            Text(
                                text = "$pageCount",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}