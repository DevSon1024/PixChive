package com.devson.pixchive.ui.reader.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderBottomBar(
    pageCount: Int,
    currentPage: Int,
    sliderDragging: Boolean,
    sliderDragValue: Float,
    showBottomOptions: Boolean,
    readingMode: String,
    readerScrollMode: String,
    mangaMode: Boolean,
    volumeKeysNavigation: Boolean,
    onRotate: () -> Unit,
    onChangeReadingMode: () -> Unit,
    onShare: () -> Unit,
    onToggleScrollMode: () -> Unit,
    onToggleMangaMode: () -> Unit,
    onToggleVolumeNav: () -> Unit,
    onToggleBottomOptions: () -> Unit,
    onSliderValueChange: (Float) -> Unit,
    onSliderValueFinished: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Floating Arrow (Separate Pill)
        Surface(
            onClick = onToggleBottomOptions,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            tonalElevation = 4.dp,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (showBottomOptions) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Floating "Dynamic" Panel (Semi-Transparent Glassmorphism)
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.75f),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                    )
                )
            ),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                // Options Row
                AnimatedVisibility(
                    visible = showBottomOptions,
                    enter = androidx.compose.animation.expandVertically(
                        animationSpec = tween(300, easing = LinearOutSlowInEasing)
                    ),
                    exit = androidx.compose.animation.shrinkVertically(
                        animationSpec = tween(300, easing = FastOutLinearInEasing)
                    )
                ) {
                    Column {
                        // Row 1: Core actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ReaderActionButton(
                                icon = Icons.AutoMirrored.Filled.RotateRight,
                                label = "ROTATE",
                                onClick = onRotate
                            )
                            ReaderActionButton(
                                icon = when (readingMode) {
                                    "fill" -> Icons.Default.CropSquare
                                    "original" -> Icons.Default.PhotoSizeSelectActual
                                    else -> Icons.Default.FitScreen
                                },
                                label = readingMode.uppercase(),
                                onClick = onChangeReadingMode
                            )
                            ReaderActionButton(
                                icon = Icons.Default.Share,
                                label = "SHARE",
                                onClick = onShare
                            )
                        }

                        // Row 2: Reading mode toggles (Webtoon + Manga)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 18.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ReaderActionButton(
                                icon = if (readerScrollMode == "webtoon")
                                    Icons.Default.ViewDay
                                else
                                    Icons.Default.SwapVert,
                                label = if (readerScrollMode == "webtoon") "PAGER" else "WEBTOON",
                                isActive = readerScrollMode == "webtoon",
                                onClick = onToggleScrollMode
                            )
                            ReaderActionButton(
                                icon = Icons.AutoMirrored.Filled.CompareArrows,
                                label = if (mangaMode) "LTR" else "MANGA",
                                isActive = mangaMode,
                                onClick = onToggleMangaMode
                            )
                            ReaderActionButton(
                                icon = if (volumeKeysNavigation) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                label = "VOL NAV",
                                isActive = volumeKeysNavigation,
                                onClick = onToggleVolumeNav
                            )
                        }
                    }
                }

                // Slider Row
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (mangaMode) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${currentPage + 1}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        SlimSlider(
                            value = if (sliderDragging) sliderDragValue else currentPage.toFloat(),
                            onValueChange = onSliderValueChange,
                            onValueChangeFinished = {
                                onSliderValueFinished(if (sliderDragging) sliderDragValue else currentPage.toFloat())
                            },
                            valueRange = 0f..maxOf(0f, (pageCount - 1).toFloat()),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        )
                        
                        Text(
                            text = "$pageCount",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlimSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
        modifier = modifier,
        thumb = {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
            )
        },
        track = { sliderPositions ->
            val fraction = if (sliderPositions.valueRange.endInclusive > sliderPositions.valueRange.start) {
                (value - sliderPositions.valueRange.start) / (sliderPositions.valueRange.endInclusive - sliderPositions.valueRange.start)
            } else {
                0f
            }
            val activeFraction = fraction.coerceIn(0f, 1f)
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(1.5.dp)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(activeFraction)
                        .fillMaxHeight()
                        .background(
                            MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(1.5.dp)
                        )
                )
            }
        }
    )
}

@Composable
fun ReaderActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    // Spring animation for press effect
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "buttonScale"
    )

    // Smooth color transition
    val backgroundColor = animateColorAsState(
        targetValue = when {
            isActive -> MaterialTheme.colorScheme.primary
            isPressed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        animationSpec = tween(200),
        label = "backgroundColor"
    ).value

    val contentColor = animateColorAsState(
        targetValue = if (isActive)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "contentColor"
    ).value

    Surface(
        onClick = {
            isPressed = true
            onClick()
        },
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        tonalElevation = if (isPressed || isActive) 4.dp else 2.dp,
        modifier = Modifier
            .scale(scale)
            .defaultMinSize(minWidth = 75.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Icon container with subtle background
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = if (isActive) 0.2f else 0.1f),
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }

    // Reset press state after animation
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(150)
            isPressed = false
        }
    }
}