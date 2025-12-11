package com.devson.pixchive.ui.reader.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    // Favorite animation
    var favoriteTrigger by remember { mutableStateOf(false) }
    val favoriteScale by animateFloatAsState(
        targetValue = if (favoriteTrigger) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "favoriteScale"
    )

    val favoriteColor by animateColorAsState(
        targetValue = if (isFavorite) Color(0xFFFF4081) else Color.White.copy(alpha = 0.9f),
        animationSpec = tween(300),
        label = "favoriteColor"
    )

    LaunchedEffect(isFavorite) {
        if (isFavorite) {
            favoriteTrigger = true
            kotlinx.coroutines.delay(300)
            favoriteTrigger = false
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Main top bar content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button with enhanced style
                Surface(
                    onClick = onNavigateBack,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title section with enhanced styling
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = chapterFolderName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (currentImageName.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = currentImageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorite Button with enhanced animation
                    Surface(
                        onClick = {
                            onToggleFavorite()
                        },
                        shape = CircleShape,
                        color = if (isFavorite)
                            Color(0xFFFF4081).copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        modifier = Modifier
                            .size(44.dp)
                            .scale(favoriteScale)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = favoriteColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Info Button
                    Surface(
                        onClick = {
                            showDetailsDialog = true
                            onMoreMenuToggle(false)
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Details",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDetailsDialog && currentImage != null) {
        ImageDetailsDialog(image = currentImage, onDismiss = { showDetailsDialog = false })
    }
}