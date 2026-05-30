package com.devson.pixchive.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.pixchive.data.local.ImageEntity
import com.devson.pixchive.ui.reader.utils.urisMatch
import com.devson.pixchive.ui.components.ViewSettingsBottomSheet
import com.devson.pixchive.ui.components.ImageGridItem
import com.devson.pixchive.ui.components.ChapterImageListItem
import com.devson.pixchive.ui.components.EmptyChapterImagesView
import com.devson.pixchive.ui.components.SkeletonGrid
import com.devson.pixchive.ui.components.SkeletonList
import com.devson.pixchive.viewmodel.FolderViewModel
import com.devson.pixchive.viewmodel.FileOperationsViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterViewScreen(
    folderId: String,
    chapterPath: String,
    onNavigateBack: () -> Unit,
    onImageClick: (Int) -> Unit,
    viewModel: FolderViewModel = viewModel(),
    fileOpsViewModel: FileOperationsViewModel = viewModel()
) {
    val context = LocalContext.current
    val layoutMode by viewModel.layoutMode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()

    var showDisplayOptions by remember { mutableStateOf(false) }

    val chapterImages = remember(chapters, chapterPath) {
        chapters.find { urisMatch(it.path, chapterPath) }?.images?.filterIsInstance<ImageEntity>() ?: emptyList()
    }
    val chapterName = remember(chapterPath) { chapterPath.substringAfterLast("/").substringAfterLast(":") }
    val onRefresh = { viewModel.refreshFolder(folderId) }

    LaunchedEffect(folderId) { viewModel.loadFolder(folderId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chapterName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { IconButton(onClick = { showDisplayOptions = true }) { Icon(Icons.Default.Tune, "Options") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                Box(modifier = Modifier.padding(top = padding.calculateTopPadding())) {
                    if (layoutMode == "grid") {
                        SkeletonGrid(columns = gridColumns)
                    } else {
                        SkeletonList()
                    }
                }
            } else if (chapterImages.isEmpty()) {
                EmptyChapterImagesView(chapterName, chapters.size)
            } else {
                if (layoutMode == "grid") {
                    val gridState = rememberLazyGridState()
                    var localColumns by remember(gridColumns) { mutableStateOf(gridColumns) }
                    var accumulatedZoom by remember { mutableFloatStateOf(1f) }

                    val animatedColumns by animateIntAsState(
                        targetValue = localColumns,
                        animationSpec = tween(300),
                        label = "columns_anim"
                    )

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(animatedColumns.coerceIn(1, 6)),
                        contentPadding = PaddingValues(
                            top = padding.calculateTopPadding() + 8.dp,
                            bottom = padding.calculateBottomPadding() + 16.dp,
                            start = 8.dp,
                            end = 8.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    var hasChangedInThisGesture = false
                                    do {
                                        val event = awaitPointerEvent()
                                        if (event.changes.size >= 2) {
                                            val zoom = event.calculateZoom()
                                            accumulatedZoom *= zoom
                                            
                                            if (!hasChangedInThisGesture) {
                                                if (accumulatedZoom > 1.25f) {
                                                    val newCols = (localColumns - 1).coerceIn(1, 6)
                                                    if (newCols != localColumns) {
                                                        localColumns = newCols
                                                        viewModel.setGridColumns(newCols)
                                                    }
                                                    hasChangedInThisGesture = true
                                                } else if (accumulatedZoom < 0.75f) {
                                                    val newCols = (localColumns + 1).coerceIn(1, 6)
                                                    if (newCols != localColumns) {
                                                        localColumns = newCols
                                                        viewModel.setGridColumns(newCols)
                                                    }
                                                    hasChangedInThisGesture = true
                                                }
                                            }
                                            event.changes.forEach { if (it.pressed) it.consume() }
                                        } else {
                                            accumulatedZoom = 1f
                                            hasChangedInThisGesture = false
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            }
                    ) {
                        itemsIndexed(chapterImages) { index, image ->
                            ImageGridItem(
                                image = image,
                                columns = animatedColumns.coerceIn(1, 6),
                                onClick = { onImageClick(index) },
                                onShareClick = {
                                    fileOpsViewModel.sharePhysicalFile(context, image.path)
                                },
                                onDeleteClick = {
                                    fileOpsViewModel.deletePhysicalFile(context, image.path) {
                                        onRefresh()
                                    }
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = padding.calculateTopPadding() + 4.dp,
                            bottom = padding.calculateBottomPadding() + 16.dp,
                            start = 8.dp,
                            end = 8.dp
                        )
                    ) {
                        itemsIndexed(chapterImages) { index, image ->
                            ChapterImageListItem(
                                image = image,
                                onClick = { onImageClick(index) },
                                onShareClick = {
                                    fileOpsViewModel.sharePhysicalFile(context, image.path)
                                },
                                onDeleteClick = {
                                    fileOpsViewModel.deletePhysicalFile(context, image.path) {
                                        onRefresh()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        if (showDisplayOptions) {
            ViewSettingsBottomSheet(
                onDismiss = { showDisplayOptions = false }, viewMode = null, layoutMode = layoutMode, gridColumns = gridColumns, sortOption = null,
                onLayoutModeChange = { viewModel.setLayoutMode(it) }, onGridColumnsChange = { viewModel.setGridColumns(it) }
            )
        }
    }
}