package com.devson.pixchive.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun ViewSettingsBottomSheet(
    onDismiss: () -> Unit,
    // State
    viewMode: String? = null, // "all_folders", "explorer", "flat" (null to hide)
    layoutMode: String, // "grid", "list"
    gridColumns: Int,
    sortOption: String? = null, // null to hide sort section
    // Callbacks
    onViewModeChange: ((String) -> Unit)? = null,
    onLayoutModeChange: (String) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onSortOptionChange: ((String) -> Unit)? = null
) {
    var showSortWheel by remember { mutableStateOf(false) }

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
                .verticalScroll(rememberScrollState())
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectionCard(
                        selected = viewMode == "all_folders",
                        title = "All Folders",
                        icon = Icons.Default.FolderCopy,
                        onClick = { onViewModeChange("all_folders") },
                        modifier = Modifier.weight(1f)
                    )
                    SelectionCard(
                        selected = viewMode == "explorer",
                        title = "Explorer",
                        icon = Icons.Default.FolderOpen,
                        onClick = { onViewModeChange("explorer") },
                        modifier = Modifier.weight(1f)
                    )
                    SelectionCard(
                        selected = viewMode == "flat" || viewMode == "files",
                        title = "Flat View",
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

            // 4. Sort By
            if (sortOption != null && onSortOptionChange != null) {
                Text(
                    text = "Sort By",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
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
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val borderColor = if (!selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null

    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = borderColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}