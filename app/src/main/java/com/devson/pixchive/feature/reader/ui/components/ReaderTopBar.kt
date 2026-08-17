package com.devson.pixchive.feature.reader.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.pixchive.core.data.ImageFile
import com.devson.pixchive.core.data.local.ImageEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(
    chapterFolderName: String,
    currentImageName: String,
    @Suppress("UNUSED_PARAMETER") showMoreMenu: Boolean = false,
    currentImage: ImageFile?,
    currentImageEntity: ImageEntity? = null,
    isFavorite: Boolean = false,
    readerScrollMode: String = "pager",
    mangaMode: Boolean = false,
    onNavigateBack: () -> Unit,
    onMoreMenuToggle: (Boolean) -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onSetReadingMode: ((scrollMode: String, isManga: Boolean) -> Unit)? = null,
    contentColor: Color = Color.White
) {
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showReadingModeMenu by remember { mutableStateOf(false) }

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
        targetValue = if (isFavorite) Color(0xFFFF4081) else contentColor.copy(alpha = 0.9f),
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

    val readingModeLabel = when {
        readerScrollMode == "webtoon" -> "Webtoon"
        mangaMode -> "Manga"
        else -> "Comic"
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Surface(
                    onClick = onNavigateBack,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title section
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
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (currentImageName.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = contentColor.copy(alpha = 0.15f),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = currentImageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor.copy(alpha = 0.85f),
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
                    // Reading Mode Selector
                    Box {
                        Surface(
                            onClick = { showReadingModeMenu = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = when {
                                        readerScrollMode == "webtoon" -> Icons.Default.SwapVert
                                        mangaMode -> Icons.AutoMirrored.Filled.CompareArrows
                                        else -> Icons.Default.ViewCarousel
                                    },
                                    contentDescription = "Reading Mode",
                                    tint = contentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = readingModeLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = contentColor
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showReadingModeMenu,
                            onDismissRequest = { showReadingModeMenu = false },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Manga Mode (RTL)") },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null)
                                },
                                trailingIcon = {
                                    if (readerScrollMode != "webtoon" && mangaMode) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                onClick = {
                                    showReadingModeMenu = false
                                    onSetReadingMode?.invoke("pager", true)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Comic Mode (LTR)") },
                                leadingIcon = {
                                    Icon(Icons.Default.ViewCarousel, contentDescription = null)
                                },
                                trailingIcon = {
                                    if (readerScrollMode != "webtoon" && !mangaMode) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                onClick = {
                                    showReadingModeMenu = false
                                    onSetReadingMode?.invoke("pager", false)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Webtoon Mode (Vertical)") },
                                leadingIcon = {
                                    Icon(Icons.Default.SwapVert, contentDescription = null)
                                },
                                trailingIcon = {
                                    if (readerScrollMode == "webtoon") {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                onClick = {
                                    showReadingModeMenu = false
                                    onSetReadingMode?.invoke("webtoon", mangaMode)
                                }
                            )
                        }
                    }

                    // Favorite Button
                    Surface(
                        onClick = { onToggleFavorite() },
                        shape = CircleShape,
                        color = if (isFavorite)
                            Color(0xFFFF4081).copy(alpha = 0.25f)
                        else
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
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
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Details",
                                tint = contentColor.copy(alpha = 0.9f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    when {
        showDetailsDialog && currentImageEntity != null ->
            ImageDetailsDialog(entity = currentImageEntity, onDismiss = { showDetailsDialog = false })
        showDetailsDialog && currentImage != null ->
            ImageDetailsDialog(image = currentImage, onDismiss = { showDetailsDialog = false })
    }
}