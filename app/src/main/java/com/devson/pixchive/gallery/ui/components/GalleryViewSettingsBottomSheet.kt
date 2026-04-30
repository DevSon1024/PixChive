package com.devson.pixchive.gallery.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devson.pixchive.gallery.data.models.GalleryViewSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryViewSettingsBottomSheet(
    layoutMode: String,
    onLayoutModeChange: (String) -> Unit,
    gridCellsIndex: Int,
    onGridCellsIndexChange: (Int) -> Unit,
    viewSettings: GalleryViewSettings,
    onViewSettingsChange: (GalleryViewSettings) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Gallery View Settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Layout Mode", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = layoutMode == "list",
                    onClick = { onLayoutModeChange("list") },
                    label = { Icon(Icons.Default.List, contentDescription = "List View") }
                )
                FilterChip(
                    selected = layoutMode == "grid",
                    onClick = { onLayoutModeChange("grid") },
                    label = { Icon(Icons.Default.GridView, contentDescription = "Grid View") }
                )
            }

            if (layoutMode == "grid") {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Grid Columns", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = gridCellsIndex == 2,
                        onClick = { onGridCellsIndexChange(2) },
                        label = { Text("2") }
                    )
                    FilterChip(
                        selected = gridCellsIndex == 1,
                        onClick = { onGridCellsIndexChange(1) },
                        label = { Text("3") }
                    )
                    FilterChip(
                        selected = gridCellsIndex == 0,
                        onClick = { onGridCellsIndexChange(0) },
                        label = { Text("4") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Fields", style = MaterialTheme.typography.labelLarge)
            Column(modifier = Modifier.padding(top = 8.dp)) {
                SettingsCheckbox(
                    label = "Thumbnail",
                    checked = viewSettings.showThumbnail,
                    onCheckedChange = { onViewSettingsChange(viewSettings.copy(showThumbnail = it)) }
                )
                SettingsCheckbox(
                    label = "File Ext.",
                    checked = viewSettings.showFileExt,
                    onCheckedChange = { onViewSettingsChange(viewSettings.copy(showFileExt = it)) }
                )
                SettingsCheckbox(
                    label = "Resolution",
                    checked = viewSettings.showResolution,
                    onCheckedChange = { onViewSettingsChange(viewSettings.copy(showResolution = it)) }
                )
                SettingsCheckbox(
                    label = "Path",
                    checked = viewSettings.showPath,
                    onCheckedChange = { onViewSettingsChange(viewSettings.copy(showPath = it)) }
                )
                SettingsCheckbox(
                    label = "Size",
                    checked = viewSettings.showSize,
                    onCheckedChange = { onViewSettingsChange(viewSettings.copy(showSize = it)) }
                )
                SettingsCheckbox(
                    label = "Date",
                    checked = viewSettings.showDate,
                    onCheckedChange = { onViewSettingsChange(viewSettings.copy(showDate = it)) }
                )
            }
        }
    }
}

@Composable
private fun SettingsCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
