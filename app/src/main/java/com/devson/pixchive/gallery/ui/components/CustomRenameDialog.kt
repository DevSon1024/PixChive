package com.devson.pixchive.gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRenameDialog(
    initialName: String,
    title: String = "Rename",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Parse initialName into base filename and extension
    val lastDotIndex = initialName.lastIndexOf('.')
    val initialBaseName = if (lastDotIndex > 0) initialName.substring(0, lastDotIndex) else initialName
    val initialExtension = if (lastDotIndex > 0) initialName.substring(lastDotIndex) else ""
    val isFolder = initialExtension.isEmpty()

    var baseName by remember { mutableStateOf(initialBaseName) }
    var extension by remember { mutableStateOf(initialExtension) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon Header
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DriveFileRenameOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isFolder) "Enter a new folder name" else "Enter a new name and extension",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = baseName,
                        onValueChange = { baseName = it },
                        modifier = Modifier.weight(if (isFolder) 1f else 0.7f),
                        shape = RoundedCornerShape(16.dp),
                        label = { Text("Name") },
                        placeholder = { Text("New name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    if (!isFolder) {
                        OutlinedTextField(
                            value = extension,
                            onValueChange = { 
                                // Ensure extension starts with a dot if not empty
                                extension = if (it.isNotEmpty() && !it.startsWith(".")) ".$it" else it
                            },
                            modifier = Modifier.weight(0.3f),
                            shape = RoundedCornerShape(16.dp),
                            label = { Text("Ext") },
                            placeholder = { Text(".ext") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    val finalName = if (isFolder) baseName.trim() else "${baseName.trim()}${extension.trim()}"
                    val canRename = baseName.isNotBlank() && finalName != initialName

                    Button(
                        onClick = { if (canRename) onConfirm(finalName) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        enabled = canRename,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Rename", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
