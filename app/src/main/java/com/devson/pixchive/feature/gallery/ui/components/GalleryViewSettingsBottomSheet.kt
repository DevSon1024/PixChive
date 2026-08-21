package com.devson.pixchive.feature.gallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.pixchive.core.data.models.GalleryViewSettings
import com.devson.pixchive.core.designsystem.component.RotarySortWheelDialog
import com.devson.pixchive.core.designsystem.component.SortDirection
import com.devson.pixchive.core.designsystem.component.formatSortField
import com.devson.pixchive.core.designsystem.component.formatSortOption
import com.devson.pixchive.core.designsystem.component.parseSortOption

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

data class AspectRatioOption(
    val label: String,
    val ratio: Float,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryViewSettingsBottomSheet(
    layoutMode: String,
    onLayoutModeChange: (String) -> Unit,
    gridCellsIndex: Int,
    onGridCellsIndexChange: (Int) -> Unit,
    viewSettings: GalleryViewSettings,
    onViewSettingsChange: (GalleryViewSettings) -> Unit,
    sortOption: String = "name_asc",
    onSortOptionChange: (String) -> Unit = {},
    isRootFolderView: Boolean = false,
    showFolderThumbnail: Boolean = true,
    onShowFolderThumbnailChange: (Boolean) -> Unit = {},
    galleryViewMode: String = "albums",
    onGalleryViewModeChange: ((String) -> Unit)? = null,
    aspectRatio: Float = 1.0f,
    onAspectRatioChange: ((Float) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortWheel by remember { mutableStateOf(false) }

    val (currentSortField, currentSortDirection) = remember(sortOption) {
        parseSortOption(sortOption)
    }

    val aspectRatioOptions = remember {
        listOf(
            AspectRatioOption("Square (1:1)", 1.0f, Icons.Default.CropSquare),
            AspectRatioOption("Portrait (3:4)", 0.75f, Icons.Default.CropPortrait),
            AspectRatioOption("Landscape (16:9)", 16f / 9f, Icons.Default.CropLandscape)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "View & Layout Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // --- View Mode Section ---
            if (onGalleryViewModeChange != null) {
                GallerySettingsSectionLabel("Gallery Mode")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentedViewModeButton(
                        label = "Albums",
                        icon = if (galleryViewMode == "albums") Icons.Filled.FolderCopy else Icons.Outlined.FolderCopy,
                        selected = galleryViewMode == "albums",
                        onClick = { onGalleryViewModeChange("albums") },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentedViewModeButton(
                        label = "Photos",
                        icon = if (galleryViewMode == "photos") Icons.Filled.Collections else Icons.Outlined.Collections,
                        selected = galleryViewMode == "photos",
                        onClick = { onGalleryViewModeChange("photos") },
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // --- Layout Type Section (Grid vs List) ---
            GallerySettingsSectionLabel("Display Layout")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SegmentedViewModeButton(
                    label = "Grid View",
                    icon = if (layoutMode == "grid") Icons.Filled.GridView else Icons.Outlined.GridView,
                    selected = layoutMode == "grid",
                    onClick = { onLayoutModeChange("grid") },
                    modifier = Modifier.weight(1f)
                )
                SegmentedViewModeButton(
                    label = "List View",
                    icon = if (layoutMode == "list") Icons.Filled.ViewAgenda else Icons.Outlined.ViewAgenda,
                    selected = layoutMode == "list",
                    onClick = { onLayoutModeChange("list") },
                    modifier = Modifier.weight(1f)
                )
            }

            // --- Grid Specific Settings (Aspect Ratio & Column Count) ---
            AnimatedVisibility(
                visible = layoutMode == "grid",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Aspect Ratio Selector
                    if (onAspectRatioChange != null) {
                        GallerySettingsSectionLabel("Cover Aspect Ratio")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            aspectRatioOptions.forEach { option ->
                                val isSelected = when (option.ratio) {
                                    1.0f -> kotlin.math.abs(aspectRatio - 1.0f) < 0.1f
                                    0.75f -> kotlin.math.abs(aspectRatio - 0.75f) < 0.1f || kotlin.math.abs(aspectRatio - 0.7f) < 0.1f
                                    else -> kotlin.math.abs(aspectRatio - (16f / 9f)) < 0.2f || kotlin.math.abs(aspectRatio - 1.5f) < 0.2f
                                }
                                SegmentedViewModeButton(
                                    label = option.label,
                                    icon = option.icon,
                                    selected = isSelected,
                                    onClick = { onAspectRatioChange(option.ratio) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Grid Columns Selector
                    GallerySettingsSectionLabel("Grid Columns")
                    Spacer(modifier = Modifier.height(8.dp))
                    val currentColumns = indexToColumns(gridCellsIndex)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2, 3, 4).forEach { columns ->
                            val isSelected = currentColumns == columns
                            val bg by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                label = "colBg"
                            )
                            val fg by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "colFg"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bg)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onGridCellsIndexChange(columnsToIndex(columns)) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$columns Columns",
                                    color = fg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // --- Sort Section ---
            GallerySettingsSectionLabel("Sorting Order")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = { showSortWheel = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val dirText = if (currentSortDirection == SortDirection.ASCENDING) "↑ Ascending" else "↓ Descending"
                Text(
                    text = "${formatSortField(currentSortField)}  •  $dirText",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // --- Visible Information Fields ---
            GallerySettingsSectionLabel("Visible Metadata Fields")
            Spacer(modifier = Modifier.height(6.dp))

            val fieldItems: List<Triple<String, Boolean, (Boolean) -> Unit>> = buildList {
                if (!isRootFolderView) {
                    add(Triple("Thumbnail", viewSettings.showThumbnail) {
                        onViewSettingsChange(viewSettings.copy(showThumbnail = it))
                    })
                }
                add(Triple("File Ext.", viewSettings.showFileExt) {
                    onViewSettingsChange(viewSettings.copy(showFileExt = it))
                })
                add(Triple("Resolution", viewSettings.showResolution) {
                    onViewSettingsChange(viewSettings.copy(showResolution = it))
                })
                add(Triple("Path", viewSettings.showPath) {
                    onViewSettingsChange(viewSettings.copy(showPath = it))
                })
                add(Triple("File Size", viewSettings.showSize) {
                    onViewSettingsChange(viewSettings.copy(showSize = it))
                })
                add(Triple("Date Modified", viewSettings.showDate) {
                    onViewSettingsChange(viewSettings.copy(showDate = it))
                })
            }

            val chunked = fieldItems.chunked(2)
            Column(modifier = Modifier.fillMaxWidth()) {
                chunked.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (label, checked, onChange) ->
                            Box(Modifier.weight(1f)) {
                                GalleryCompactMetadataToggle(
                                    label = label,
                                    checked = checked,
                                    onCheckedChange = onChange
                                )
                            }
                        }
                        repeat(2 - rowItems.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }

            if (isRootFolderView) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                GallerySettingsSectionLabel("Folder Display")
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onShowFolderThumbnailChange(!showFolderThumbnail) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Folder Preview Thumbnail",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
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
            }
        }
    }

    if (showSortWheel) {
        RotarySortWheelDialog(
            currentSortField = currentSortField,
            sortDirection = currentSortDirection,
            onSortFieldSelected = { field ->
                onSortOptionChange(formatSortOption(field, currentSortDirection))
            },
            onSortOrderToggled = { direction ->
                onSortOptionChange(formatSortOption(currentSortField, direction))
            },
            onDismissRequest = { showSortWheel = false }
        )
    }
}

@Composable
private fun GallerySettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SegmentedViewModeButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(180),
        label = "btnBg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "btnFg"
    )
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, border),
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = fg,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GalleryCompactMetadataToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (checked) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}