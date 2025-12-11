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
import androidx.core.content.FileProvider
import com.devson.pixchive.data.ImageFile
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(
    chapterFolderName: String,
    currentImageName: String,
    showMoreMenu: Boolean,
    currentImage: ImageFile?,
    isFavorite: Boolean = false,
    onNavigateBack: () -> Unit,
    onMoreMenuToggle: (Boolean) -> Unit,
    onToggleFavorite: () -> Unit = {}
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
                        text = chapterFolderName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentImageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
            },
            actions = {
                // Favorite Button
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Red else Color.White
                    )
                }

                // Info Button
                IconButton(onClick = { showDetailsDialog = true; onMoreMenuToggle(false) }) {
                    Icon(Icons.Default.Info, "Details", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }

    if (showDetailsDialog && currentImage != null) {
        ImageDetailsDialog(image = currentImage, onDismiss = { showDetailsDialog = false })
    }
}