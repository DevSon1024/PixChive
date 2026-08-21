package com.devson.pixchive.feature.reader.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.devson.pixchive.core.data.FolderMetadata
import com.devson.pixchive.core.designsystem.component.bouncyClickable
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

/**
 * 8-lobed organic scalloped shape matching modern expressive Material 3 aesthetic.
 */
val FlowerScallopShape = GenericShape { size, _ ->
    val radius = size.minDimension / 2f
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val lobes = 8
    val innerRadius = radius * 0.86f
    val outerRadius = radius

    val totalPoints = lobes * 4
    val angleStep = (2.0 * Math.PI) / totalPoints

    for (i in 0 until totalPoints) {
        // Smooth sine wave modulation for rounded flower petals
        val factor = 0.5 + 0.5 * cos(i * angleStep * lobes)
        val currentRadius = (innerRadius + (outerRadius - innerRadius) * factor).toFloat()
        val angle = i * angleStep - Math.PI / 2.0
        val x = (centerX + currentRadius * cos(angle)).toFloat()
        val y = (centerY + currentRadius * sin(angle)).toFloat()

        if (i == 0) {
            moveTo(x, y)
        } else {
            lineTo(x, y)
        }
    }
    close()
}

/**
 * Premium Hero Header for ChapterViewScreen & comic folders.
 * Features a large edge-to-edge cover artwork background with a smooth vertical gradient
 * blending seamlessly into the screen background, clear typography hierarchy, and a stylized FAB.
 */
@Composable
fun FolderHeroHeader(
    folderName: String,
    coverImageUri: Any?,
    totalImages: Int,
    folderSizeFormatted: String,
    onNavigateBack: () -> Unit,
    onReadClick: () -> Unit,
    modifier: Modifier = Modifier,
    overlineText: String = "COMIC FOLDER",
    lastReadProgress: Float = 0f,
    lastReadPage: Int = 0,
    onOptionsClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val headerHeight = remember(configuration.screenHeightDp) {
        (configuration.screenHeightDp * 0.40f).coerceIn(280f, 380f).dp
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
        // 1. Background Cover Image
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = "$folderName Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
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
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
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
                            Color.Black.copy(alpha = 0.40f),
                            Color.Black.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 3. Smooth Bottom Gradient Scrim (Melt into screen background)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.35f to Color.Transparent,
                        0.60f to backgroundColor.copy(alpha = 0.45f),
                        0.82f to backgroundColor.copy(alpha = 0.88f),
                        1.0f to backgroundColor
                    )
                )
        )

        // 4. Top Navigation Bar with Circular Glassmorphic Buttons
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

            // Optional Options / Settings Button
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
            // Left Column: Overline, Title, Subtitle
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

                // Large Bold Folder Name
                Text(
                    text = folderName.ifEmpty { "Comic Folder" },
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Subtitle Row: Images Count • Size • Reading Progress
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val countText = if (totalImages == 1) "1 Image" else "$totalImages Images"
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (folderSizeFormatted.isNotBlank() && folderSizeFormatted != "0 B") {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = folderSizeFormatted,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (lastReadProgress > 0f) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Text(
                                text = "${(lastReadProgress * 100).toInt()}% read",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Right Action Button: Stylized Organic Flower / Squircle FAB for Read / Resume
            Surface(
                shape = FlowerScallopShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(62.dp)
                    .bouncyClickable(onClick = onReadClick)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (lastReadPage > 0) Icons.Default.PlayArrow else Icons.Default.AutoStories,
                        contentDescription = if (lastReadPage > 0) "Resume Reading" else "Start Reading",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

/**
 * Convenient overload accepting [FolderMetadata] directly.
 */
@Composable
fun FolderHeroHeader(
    metadata: FolderMetadata,
    onNavigateBack: () -> Unit,
    onReadClick: () -> Unit,
    modifier: Modifier = Modifier,
    overlineText: String = "COMIC FOLDER",
    onOptionsClick: (() -> Unit)? = null
) {
    FolderHeroHeader(
        folderName = metadata.folderName,
        coverImageUri = metadata.coverImageUri,
        totalImages = metadata.totalImages,
        folderSizeFormatted = metadata.folderSizeFormatted,
        lastReadProgress = metadata.lastReadProgress,
        lastReadPage = metadata.lastReadPage,
        onNavigateBack = onNavigateBack,
        onReadClick = onReadClick,
        modifier = modifier,
        overlineText = overlineText,
        onOptionsClick = onOptionsClick
    )
}
