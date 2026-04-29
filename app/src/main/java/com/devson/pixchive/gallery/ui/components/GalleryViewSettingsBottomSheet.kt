package com.devson.pixchive.gallery.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryViewSettingsBottomSheet(
    layoutMode: String,
    onLayoutModeChange: (String) -> Unit,
    gridCellsIndex: Int,
    onGridCellsIndexChange: (Int) -> Unit,
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
                    label = { Text("List") }
                )
                FilterChip(
                    selected = layoutMode == "grid",
                    onClick = { onLayoutModeChange("grid") },
                    label = { Text("Grid") }
                )
            }

            if (layoutMode == "grid") {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Grid Columns", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Index mapping: 0 -> 4 columns, 1 -> 3 columns, 2 -> 2 columns
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
        }
    }
}
