package com.devson.pixchive.feature.gallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devson.pixchive.core.data.models.GalleryViewSettings
import com.devson.pixchive.core.designsystem.component.RotarySortWheelDialog
import com.devson.pixchive.core.designsystem.component.SortDirection
import com.devson.pixchive.core.designsystem.component.SortField
import com.devson.pixchive.core.designsystem.component.formatSortField
import com.devson.pixchive.core.designsystem.component.formatSortOption
import com.devson.pixchive.core.designsystem.component.parseSortOption
import com.devson.pixchive.core.designsystem.theme.PixchiveTheme
import kotlin.math.abs

// gridCellsIndex is the PinchZoom index: 0=Fixed(4), 1=Fixed(3), 2=Fixed(2)
// gridColumnCount is the actual visible column count: 4, 3, or 2
private fun indexToColumns(index: Int): Int = when (index) {
    0 -> 4
    1 -> 3
    else -> 2
}

private fun columnsToIndex(columns: Int): Int = when (columns) {
    4 -> 0
    3 -> 1
    else -> 2
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryViewSettingsBottomSheet(
    onDismiss: () -> Unit,
    // State
    layoutMode: String = "grid",
    gridCellsIndex: Int = 1,
    gridColumns: Int? = null,
    viewSettings: GalleryViewSettings = GalleryViewSettings(),
    sortOption: String? = "name_asc",
    isRootFolderView: Boolean = false,
    showFolderThumbnail: Boolean = true,
    galleryViewMode: String? = null,
    aspectRatio: Float = 1.0f,
    // Callbacks
    onLayoutModeChange: (String) -> Unit = {},
    onGridCellsIndexChange: ((Int) -> Unit)? = null,
    onGridColumnsChange: ((Int) -> Unit)? = null,
    onViewSettingsChange: (GalleryViewSettings) -> Unit = {},
    onSortOptionChange: ((String) -> Unit)? = null,
    onShowFolderThumbnailChange: ((Boolean) -> Unit)? = null,
    onGalleryViewModeChange: ((String) -> Unit)? = null,
    onAspectRatioChange: ((Float) -> Unit)? = null
) {
    var showSortWheel by remember { mutableStateOf(false) }

    val currentColumns = remember(gridColumns, gridCellsIndex) {
        gridColumns ?: indexToColumns(gridCellsIndex)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Display Options",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(20.dp))

            // 1. View Mode (Albums vs Photos)
            if (galleryViewMode != null && onGalleryViewModeChange != null) {
                Text(
                    text = "View Mode",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SelectionCard(
                        selected = galleryViewMode == "albums",
                        title = "Albums",
                        icon = if (galleryViewMode == "albums") Icons.Default.FolderCopy else Icons.Outlined.FolderCopy,
                        onClick = { onGalleryViewModeChange("albums") },
                        modifier = Modifier.weight(1f)
                    )
                    SelectionCard(
                        selected = galleryViewMode == "photos" || galleryViewMode == "flat",
                        title = "Photos",
                        icon = if (galleryViewMode == "photos" || galleryViewMode == "flat") Icons.Default.Collections else Icons.Outlined.Collections,
                        onClick = { onGalleryViewModeChange("photos") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 2. Layout (Grid vs List)
            Text(
                text = "Layout",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SelectionCard(
                    selected = layoutMode == "grid",
                    title = "Grid",
                    icon = if (layoutMode == "grid") Icons.Default.GridView else Icons.Outlined.GridView,
                    onClick = { onLayoutModeChange("grid") },
                    modifier = Modifier.weight(1f)
                )
                SelectionCard(
                    selected = layoutMode == "list",
                    title = "List",
                    icon = if (layoutMode == "list") Icons.AutoMirrored.Filled.ViewList else Icons.AutoMirrored.Outlined.ViewList,
                    onClick = { onLayoutModeChange("list") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            // 3. Grid Columns (Only in Grid mode)
            if (layoutMode == "grid") {
                val haptic = LocalHapticFeedback.current
                Text(
                    text = "Grid Columns",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2, 3, 4).forEach { columns ->
                        val isSelected = currentColumns == columns
                        val containerColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            label = "col_container"
                        )
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "col_content"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(containerColor)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (!isSelected) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onGridColumnsChange?.invoke(columns)
                                        onGridCellsIndexChange?.invoke(columnsToIndex(columns))
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$columns",
                                color = contentColor,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 4. Cover Aspect Ratio (Only in Grid mode and when onAspectRatioChange provided)
            if (layoutMode == "grid" && onAspectRatioChange != null) {
                Text(
                    text = "Cover Ratio",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectionCard(
                        selected = abs(aspectRatio - 0.75f) < 0.1f || abs(aspectRatio - 0.7f) < 0.1f,
                        title = "Portrait",
                        icon = Icons.Default.CropPortrait,
                        onClick = { onAspectRatioChange(0.75f) },
                        modifier = Modifier.weight(1f)
                    )
                    SelectionCard(
                        selected = abs(aspectRatio - 1.0f) < 0.1f,
                        title = "Square",
                        icon = Icons.Default.CropSquare,
                        onClick = { onAspectRatioChange(1.0f) },
                        modifier = Modifier.weight(1f)
                    )
                    SelectionCard(
                        selected = abs(aspectRatio - (16f / 9f)) < 0.2f || abs(aspectRatio - 1.5f) < 0.2f,
                        title = "Landscape",
                        icon = Icons.Default.CropLandscape,
                        onClick = { onAspectRatioChange(1.5f) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 5. Display Elements
            Text(
                text = "Display Elements",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Show File Extension Switch (Thumbnail is always enabled by default)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val newShowExt = !viewSettings.showFileExt
                        onViewSettingsChange(viewSettings.copy(showThumbnail = true, showFileExt = newShowExt))
                    }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Show File Extension",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Display file formats (.jpg, .png, etc.) on image items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = viewSettings.showFileExt,
                    onCheckedChange = { newShowExt ->
                        onViewSettingsChange(viewSettings.copy(showThumbnail = true, showFileExt = newShowExt))
                    }
                )
            }

            // Optional Folder Preview Thumbnail Switch for Root/Albums views
            if (isRootFolderView && onShowFolderThumbnailChange != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onShowFolderThumbnailChange(!showFolderThumbnail) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Folder Preview Thumbnail",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Display photo artwork on folder covers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showFolderThumbnail,
                        onCheckedChange = onShowFolderThumbnailChange
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // 6. Sorting Order
            if (sortOption != null && onSortOptionChange != null) {
                Text(
                    text = "Sort By",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))

                val fieldAndDir = parseSortOption(sortOption)

                OutlinedButton(
                    onClick = { showSortWheel = true },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    val dirText = if (fieldAndDir.second == SortDirection.ASCENDING) {
                        if (fieldAndDir.first == SortField.DATE) "Oldest" else "A to Z"
                    } else {
                        if (fieldAndDir.first == SortField.DATE) "Newest" else "Z to A"
                    }
                    Text(
                        text = "${formatSortField(fieldAndDir.first)} · $dirText",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (showSortWheel) {
                    RotarySortWheelDialog(
                        currentSortField = fieldAndDir.first,
                        sortDirection = fieldAndDir.second,
                        onSortFieldSelected = { newField ->
                            val newOption = formatSortOption(newField, fieldAndDir.second)
                            onSortOptionChange(newOption)
                        },
                        onSortOrderToggled = { newDir ->
                            val newOption = formatSortOption(fieldAndDir.first, newDir)
                            onSortOptionChange(newOption)
                        },
                        onDismissRequest = { showSortWheel = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionCard(
    selected: Boolean,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        label = "sel_container"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        label = "sel_content"
    )
    val borderColor = if (!selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)

    Surface(
        onClick = {
            if (!selected) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onClick()
        },
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = borderColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GalleryViewSettingsBottomSheetPreview() {
    PixchiveTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Gallery Display Options Preview", style = MaterialTheme.typography.titleLarge)
        }
    }
}