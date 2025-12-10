package com.devson.pixchive.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayOptionsSheet(
    onDismiss: () -> Unit,
    // State
    viewMode: String? = null, // "explorer", "flat" (null to hide)
    layoutMode: String, // "grid", "list"
    gridColumns: Int,
    sortOption: String? = null, // null to hide sort section
    // Callbacks
    onViewModeChange: ((String) -> Unit)? = null,
    onLayoutModeChange: (String) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onSortOptionChange: ((String) -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Display Options",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 1. View Mode (Only if enabled)
            if (viewMode != null && onViewModeChange != null) {
                Text(
                    text = "View Mode",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SelectionCard(
                        selected = viewMode == "explorer",
                        title = "Explorer",
                        icon = Icons.Default.FolderOpen,
                        onClick = { onViewModeChange("explorer") },
                        modifier = Modifier.weight(1f)
                    )
                    SelectionCard(
                        selected = viewMode == "flat",
                        title = "Flat Gallery",
                        icon = Icons.Default.Collections,
                        onClick = { onViewModeChange("flat") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 2. Layout
            Text(
                text = "Layout",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SelectionCard(
                    selected = layoutMode == "grid",
                    title = "Grid",
                    icon = Icons.Default.GridView,
                    onClick = { onLayoutModeChange("grid") },
                    modifier = Modifier.weight(1f)
                )
                SelectionCard(
                    selected = layoutMode == "list",
                    title = "List",
                    icon = Icons.AutoMirrored.Filled.ViewList,
                    onClick = { onLayoutModeChange("list") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // 3. Grid Columns Slider (Only visible in Grid mode)
            if (layoutMode == "grid") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Grid Columns",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$gridColumns",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = gridColumns.toFloat(),
                    onValueChange = { onGridColumnsChange(it.toInt()) },
                    valueRange = 1f..6f,
                    steps = 4, // 1 to 6
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 4. Sort By (Only if enabled)
            if (sortOption != null && onSortOptionChange != null) {
                Text(
                    text = "Sort By",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                SortOptionRow("Name (A-Z)", sortOption == "name_asc") { onSortOptionChange("name_asc") }
                SortOptionRow("Name (Z-A)", sortOption == "name_desc") { onSortOptionChange("name_desc") }
                SortOptionRow("Date Modified (Newest)", sortOption == "date_newest") { onSortOptionChange("date_newest") }
                SortOptionRow("Date Modified (Oldest)", sortOption == "date_oldest") { onSortOptionChange("date_oldest") }
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
    Surface(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (!selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SortOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null // Handled by Row click
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}