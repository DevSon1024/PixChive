package com.devson.pixchive.ui.reader.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.pixchive.data.ImageFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(
    folderName: String,
    chapterName: String,
    showMoreMenu: Boolean,
    currentImage: ImageFile?,
    onNavigateBack: () -> Unit,
    onMoreMenuToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var showDetailsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.85f))
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = folderName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = chapterName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(onClick = { /* TODO: Bookmark */ }) {
                    Icon(
                        Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = Color.White
                    )
                }

                Box {
                    IconButton(onClick = { onMoreMenuToggle(!showMoreMenu) }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { onMoreMenuToggle(false) }
                    ) {
                        // Share
                        DropdownMenuItem(
                            text = { Text("Share Image") },
                            leadingIcon = {
                                Icon(Icons.Default.Share, contentDescription = null)
                            },
                            onClick = {
                                currentImage?.let { shareImage(context, it) }
                                onMoreMenuToggle(false)
                            }
                        )

                        // Image Details (NEW - Opens Dialog)
                        DropdownMenuItem(
                            text = { Text("Image Details") },
                            leadingIcon = {
                                Icon(Icons.Default.Info, contentDescription = null)
                            },
                            onClick = {
                                showDetailsDialog = true
                                onMoreMenuToggle(false)
                            }
                        )

                        Divider()

                        // Delete
                        DropdownMenuItem(
                            text = { Text("Delete Image", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                currentImage?.let { deleteImage(context, it) }
                                onMoreMenuToggle(false)
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }

    // Image Details Dialog
    if (showDetailsDialog && currentImage != null) {
        ImageDetailsDialog(
            image = currentImage,
            onDismiss = { showDetailsDialog = false }
        )
    }
}

private fun shareImage(context: Context, image: ImageFile) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, image.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share image", Toast.LENGTH_SHORT).show()
    }
}

private fun deleteImage(context: Context, image: ImageFile) {
    // TODO: Implement delete with confirmation dialog
    Toast.makeText(context, "Delete functionality coming soon", Toast.LENGTH_SHORT).show()
}