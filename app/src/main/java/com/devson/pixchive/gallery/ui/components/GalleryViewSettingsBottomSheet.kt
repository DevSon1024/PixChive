package com.devson.pixchive.gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewAgenda
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
import com.devson.pixchive.gallery.data.models.GalleryViewSettings
import com.devson.pixchive.ui.components.RotarySortWheelDialog
import com.devson.pixchive.ui.components.SortDirection
import com.devson.pixchive.ui.components.SortField
import com.devson.pixchive.ui.components.formatSortField
import com.devson.pixchive.ui.components.formatSortOption
import com.devson.pixchive.ui.components.parseSortOption

import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.foundation.layout.Arrangement

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
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var showSortWheel by remember { mutableStateOf(false) }

    val (currentSortField, currentSortDirection) = remember(sortOption) {
        parseSortOption(sortOption)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            Text(
                "View Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (onGalleryViewModeChange != null) {
                GallerySettingsSectionLabel("View Mode")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    GalleryIconToggleButton(
                        label = "Albums",
                        selected = galleryViewMode == "albums",
                        selectedIcon = Icons.Filled.FolderCopy,
                        unselectedIcon = Icons.Outlined.FolderCopy,
                        modifier = Modifier.weight(1f),
                        onClick = { onGalleryViewModeChange("albums") }
                    )
                    GalleryIconToggleButton(
                        label = "Photos",
                        selected = galleryViewMode == "all_images",
                        selectedIcon = Icons.Filled.Collections,
                        unselectedIcon = Icons.Outlined.Collections,
                        modifier = Modifier.weight(1f),
                        onClick = { onGalleryViewModeChange("all_images") }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    GallerySettingsSectionLabel("Layout")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        GalleryIconToggleButton(
                            label = "List",
                            selected = layoutMode == "list",
                            selectedIcon = Icons.Filled.ViewAgenda,
                            unselectedIcon = Icons.Outlined.ViewAgenda,
                            modifier = Modifier.weight(1f),
                            onClick = { onLayoutModeChange("list") }
                        )
                        GalleryIconToggleButton(
                            label = "Grid",
                            selected = layoutMode == "grid",
                            selectedIcon = Icons.Filled.GridView,
                            unselectedIcon = Icons.Outlined.GridView,
                            modifier = Modifier.weight(1f),
                            onClick = { onLayoutModeChange("grid") }
                        )
                    }
                }
            }

            if (layoutMode == "grid") {
                Spacer(modifier = Modifier.height(10.dp))
                GallerySettingsSectionLabel("Grid Columns")
                Spacer(modifier = Modifier.height(6.dp))
                // Show actual column counts 2, 3, 4 mapped to pinch-zoom indices 2, 1, 0
                val currentColumns = indexToColumns(gridCellsIndex)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 3, 4).forEach { columns ->
                            val isSelected = currentColumns == columns
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onGridCellsIndexChange(columnsToIndex(columns)) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = columns.toString(),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            // Sort
            GallerySettingsSectionLabel("Sort By")
            OutlinedButton(
                onClick = { showSortWheel = true },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                val dirText = if (currentSortDirection == SortDirection.ASCENDING) "↑ Ascending" else "↓ Descending"
                Text(
                    "${formatSortField(currentSortField)}  $dirText",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            // Fields
            GallerySettingsSectionLabel("Fields")
            Spacer(modifier = Modifier.height(4.dp))

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
                add(Triple("Size", viewSettings.showSize) {
                    onViewSettingsChange(viewSettings.copy(showSize = it))
                })
                add(Triple("Date", viewSettings.showDate) {
                    onViewSettingsChange(viewSettings.copy(showDate = it))
                })
            }

            val chunked = fieldItems.chunked(3)
            Column(modifier = Modifier.fillMaxWidth()) {
                chunked.forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { (label, checked, onChange) ->
                            Box(Modifier.weight(1f)) {
                                GalleryCompactMetadataToggle(
                                    label = label,
                                    checked = checked,
                                    onCheckedChange = onChange
                                )
                            }
                        }
                        repeat(3 - rowItems.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isRootFolderView) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                GallerySettingsSectionLabel("Advanced")
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowFolderThumbnailChange(!showFolderThumbnail) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Folder Thumbnail",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Display a photo preview on folder icons",
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

            Spacer(modifier = Modifier.height(24.dp))
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
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun GalleryIconToggleButton(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val color = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    val icon = if (selected) selectedIcon else unselectedIcon
    val fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = fontWeight,
            color = color,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun GalleryCompactMetadataToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(32.dp)
        )
        Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}
