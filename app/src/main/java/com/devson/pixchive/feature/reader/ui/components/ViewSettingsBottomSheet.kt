package com.devson.pixchive.feature.reader.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devson.pixchive.core.designsystem.component.RotarySortWheelDialog
import com.devson.pixchive.core.designsystem.component.SortDirection
import com.devson.pixchive.core.designsystem.component.SortField
import com.devson.pixchive.core.designsystem.component.formatSortField
import com.devson.pixchive.core.designsystem.component.formatSortOption
import com.devson.pixchive.core.designsystem.component.parseSortOption
import com.devson.pixchive.core.designsystem.theme.PixchiveTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewSettingsBottomSheet(
    onDismiss: () -> Unit,
    // State
    viewMode: String? = null, // "all_folders", "flat" (null to hide)
    layoutMode: String, // "grid", "list"
    gridColumns: Int,
    sortOption: String? = null, // null to hide sort section
    coverAspectRatio: Float = 0.7f,
    showUnreadBadges: Boolean = true,
    showProgressBars: Boolean = true,
    // Callbacks
    onViewModeChange: ((String) -> Unit)? = null,
    onLayoutModeChange: (String) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onSortOptionChange: ((String) -> Unit)? = null,
    onCoverAspectRatioChange: ((Float) -> Unit)? = null,
    onShowUnreadBadgesChange: ((Boolean) -> Unit)? = null,
    onShowProgressBarsChange: ((Boolean) -> Unit)? = null
) {
    var showSortWheel by remember { mutableStateOf(false) }

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

            // 1. View Mode (Only if enabled)
            if (viewMode != null && onViewModeChange != null) {
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
                        selected = viewMode == "all_folders",
                        title = "All Folders",
                        icon = if (viewMode == "all_folders") Icons.Default.FolderCopy else Icons.Outlined.FolderCopy,
                        onClick = { onViewModeChange("all_folders") },
                        modifier = Modifier.weight(1f)
                    )
                    SelectionCard(
                        selected = viewMode == "flat" || viewMode == "files",
                        title = "Flat View",
                        icon = if (viewMode == "flat" || viewMode == "files") Icons.Default.Collections else Icons.Outlined.Collections,
                        onClick = { onViewModeChange("flat") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 2. Layout
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

            // 3. Grid Columns (Only visible in Grid mode)
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
                    listOf(1, 2, 3, 4).forEach { columns ->
                        val isSelected = gridColumns == columns
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
                                        onGridColumnsChange(columns)
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

            // 4. Cover Aspect Ratio (Only when in Grid mode and callback provided)
            if (layoutMode == "grid" && onCoverAspectRatioChange != null) {
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
                        selected = kotlin.math.abs(coverAspectRatio - 0.7f) < 0.05f,
                        title = "Portrait",
                        icon = Icons.Default.CropPortrait,
                        onClick = { onCoverAspectRatioChange(0.7f) },
                        modifier = Modifier.weight(1f)
                    )
                    SelectionCard(
                        selected = kotlin.math.abs(coverAspectRatio - 1.0f) < 0.05f,
                        title = "Square",
                        icon = Icons.Default.CropSquare,
                        onClick = { onCoverAspectRatioChange(1.0f) },
                        modifier = Modifier.weight(1f)
                    )
                    SelectionCard(
                        selected = kotlin.math.abs(coverAspectRatio - 1.5f) < 0.05f,
                        title = "Landscape",
                        icon = Icons.Default.CropLandscape,
                        onClick = { onCoverAspectRatioChange(1.5f) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 5. Card Elements (Badges & Progress Bars)
            if (onShowUnreadBadgesChange != null || onShowProgressBarsChange != null) {
                Text(
                    text = "Card Elements",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (onShowUnreadBadgesChange != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onShowUnreadBadgesChange(!showUnreadBadges) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Item Count Badges",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Show item count badge on covers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showUnreadBadges,
                            onCheckedChange = onShowUnreadBadgesChange
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (onShowProgressBarsChange != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onShowProgressBarsChange(!showProgressBars) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reading Progress Bars",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Show progress indicator under card titles",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showProgressBars,
                            onCheckedChange = onShowProgressBarsChange
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 6. Sort By
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
private fun ViewSettingsBottomSheetPreview() {
    PixchiveTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Display Options Preview", style = MaterialTheme.typography.titleLarge)
        }
    }
}
