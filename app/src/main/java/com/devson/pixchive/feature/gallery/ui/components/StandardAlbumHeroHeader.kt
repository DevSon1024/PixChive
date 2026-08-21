package com.devson.pixchive.feature.gallery.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.devson.pixchive.core.designsystem.component.bouncyClickable
import java.io.File

/**
 * Premium Material 3 Hero Header for Standard Gallery Image Albums.
 * Features a blurred latest photo background with a smooth vertical gradient scrim,
 * prominent typography hierarchy, glassmorphic top navigation buttons, and an action FAB.
 */
@Composable
fun StandardAlbumHeroHeader(
    albumName: String,
    coverImageUri: Any?,
    totalImages: Int,
    albumSizeFormatted: String,
    onNavigateBack: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    overlineText: String = "PHOTO ALBUM",
    onOptionsClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val headerHeight = remember(configuration.screenHeightDp) {
        (configuration.screenHeightDp * 0.38f).coerceIn(260f, 360f).dp
    }

    val backgroundColor = MaterialTheme.colorScheme.background

    val imageRequest = remember(coverImageUri) {
        when (coverImageUri) {
            is File -> ImageRequest.Builder(context)
                .data(coverImageUri)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .allowHardware(true)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(300)
                .build()
            is String -> if (coverImageUri.isNotBlank()) {
                ImageRequest.Builder(context)
                    .data(if (coverImageUri.startsWith("/")) File(coverImageUri) else coverImageUri)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .allowHardware(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(300)
                    .build()
            } else null
            else -> coverImageUri?.let {
                ImageRequest.Builder(context)
                    .data(it)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .allowHardware(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(300)
                    .build()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight)
            .background(backgroundColor)
    ) {
        // 1. Blurred Background Image
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = "$albumName Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 16.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .align(Alignment.TopCenter)
            )
        } else {
            // Elegant placeholder pattern if cover is unavailable
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                            )
                        )
                    )
            )
        }

        // 2. Top Scrim for Status Bar & Navigation Buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 3. Smooth Bottom Gradient Scrim (Melts into screen background)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.30f to Color.Transparent,
                        0.60f to backgroundColor.copy(alpha = 0.55f),
                        0.82f to backgroundColor.copy(alpha = 0.90f),
                        1.0f to backgroundColor
                    )
                )
        )

        // 4. Top Navigation Bar with Circular Glassmorphic Buttons (statusBarsPadding)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Back Button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .size(44.dp)
                    .bouncyClickable(onClick = onNavigateBack)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Options / Settings Button
            if (onOptionsClick != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .size(44.dp)
                        .bouncyClickable(onClick = onOptionsClick)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Options",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 5. Foreground Content: Metadata (Bottom-Start) & Action FAB (Bottom-End)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Left Column: Overline, Album Name, Photo Count & Size
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Overline Text
                Text(
                    text = overlineText.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Prominent Album Name
                Text(
                    text = albumName.ifEmpty { "Photo Album" },
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Subtitle Row: Total Photos • Total Size
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val countText = if (totalImages == 1) "1 Photo" else "$totalImages Photos"
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (albumSizeFormatted.isNotBlank() && albumSizeFormatted != "0 B") {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = albumSizeFormatted,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Right Action Button: Stylized Circular FAB
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(58.dp)
                    .bouncyClickable(onClick = onActionClick)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "View Photos",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
