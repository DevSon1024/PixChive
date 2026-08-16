package com.devson.pixchive.core.designsystem.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devson.pixchive.core.designsystem.theme.PixchiveTheme

/**
 * Creates a theme-aware animated shimmer brush.
 */
@Composable
fun shimmerBrush(
    showShimmer: Boolean = true,
    targetValue: Float = 1000f,
    baseColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    highlightColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            baseColor,
            highlightColor,
            baseColor
        )

        val transition = rememberInfiniteTransition(label = "shimmerTransition")
        val translateAnimation by transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerTranslate"
        )

        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation, y = translateAnimation)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

/**
 * Reusable modifier to apply animated skeleton shimmer with a clipped shape.
 */
fun Modifier.shimmerEffect(
    shape: Shape = RoundedCornerShape(8.dp),
    showShimmer: Boolean = true
): Modifier = composed {
    this
        .clip(shape)
        .background(shimmerBrush(showShimmer = showShimmer))
}

@Composable
fun SkeletonHome(
    layoutMode: String = "grid",
    columns: Int = 2,
    showHistory: Boolean = true,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (layoutMode == "grid") columns else 1),
        contentPadding = PaddingValues(bottom = 88.dp),
        modifier = modifier.fillMaxSize()
    ) {
        if (showHistory) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SkeletonSectionHeader(title = "Jump Back In", icon = Icons.Default.History)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(3) {
                        SkeletonHistoryCard()
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SkeletonSectionHeader(title = "My Folders", icon = Icons.Default.FolderOpen)
        }

        if (layoutMode == "grid") {
            items(8) {
                Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    SkeletonGridItem()
                }
            }
        } else {
            items(5) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    SkeletonListItem()
                }
            }
        }
    }
}

@Composable
private fun SkeletonSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SkeletonHistoryCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .width(140.dp)
            .aspectRatio(0.7f),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(shimmerBrush())
        )
    }
}

@Composable
fun SkeletonList(
    count: Int = 10,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(count) {
            SkeletonListItem()
        }
    }
}

@Composable
fun SkeletonLoadingView(
    layoutMode: String = "grid",
    columns: Int = 3,
    count: Int = 15,
    modifier: Modifier = Modifier
) {
    if (layoutMode == "list") {
        SkeletonList(count = count, modifier = modifier)
    } else {
        SkeletonGrid(columns = columns.coerceIn(1, 6), count = count, modifier = modifier)
    }
}

@Composable
fun SkeletonGrid(
    columns: Int = 3,
    count: Int = 15,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(count) {
            SkeletonGridItem()
        }
    }
}

@Composable
fun SkeletonListItem(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shimmerEffect(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(18.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .height(14.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
fun SkeletonGridItem(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shimmerEffect(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .shimmerEffect(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SkeletonListItemPreview() {
    PixchiveTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SkeletonListItem()
            Spacer(modifier = Modifier.height(12.dp))
            SkeletonListItem()
        }
    }
}