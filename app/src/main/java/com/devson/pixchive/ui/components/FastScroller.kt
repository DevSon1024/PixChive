package com.devson.pixchive.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
fun VerticalFastScroller(
    listState: LazyGridState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        content()

        val scope = rememberCoroutineScope()
        var isDragging by remember { mutableStateOf(false) }
        var isVisible by remember { mutableStateOf(false) }

        val firstVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
        val totalItemsCount by remember { derivedStateOf { listState.layoutInfo.totalItemsCount } }

        val scrollProgress by remember(firstVisibleItemIndex, totalItemsCount) {
            derivedStateOf {
                if (totalItemsCount == 0) 0f
                else firstVisibleItemIndex.toFloat() / totalItemsCount.toFloat()
            }
        }

        // Logic to handle auto-hide with delay
        LaunchedEffect(isDragging, listState.isScrollInProgress) {
            if (isDragging || listState.isScrollInProgress) {
                isVisible = true
            } else {
                // Wait for 3 seconds before hiding
                delay(3000)
                isVisible = false
            }
        }

        // Fade out based on isVisible state
        val alpha by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 300),
            label = "alpha"
        )

        if (totalItemsCount > 0) {
            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(60.dp) // Wide touch area for ease of use
                    .padding(end = 4.dp, top = 8.dp, bottom = 8.dp)
            ) {
                val trackHeightPx = constraints.maxHeight.toFloat()

                // Fixed size thumb for Google Files style (pill shape)
                val thumbHeight = 90.dp
                val thumbHeightPx = with(LocalDensity.current) { thumbHeight.toPx() }

                // Calculate Offset
                val thumbOffset = scrollProgress * (trackHeightPx - thumbHeightPx)

                // The Visual Thumb (Google Files Style)
                Surface(
                    modifier = Modifier
                        .offset(y = with(LocalDensity.current) { thumbOffset.toDp() })
                        .align(Alignment.TopEnd)
                        .size(width = 32.dp, height = thumbHeight)
                        .alpha(alpha)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Invisible Touch Listener (Full Height)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                isDragging = true
                                val newOffset = (thumbOffset + delta).coerceIn(0f, trackHeightPx - thumbHeightPx)
                                val newProgress = newOffset / (trackHeightPx - thumbHeightPx)
                                val newItemIndex = (newProgress * totalItemsCount).toInt()
                                scope.launch {
                                    listState.scrollToItem(newItemIndex)
                                }
                            },
                            onDragStopped = { isDragging = false }
                        )
                )
            }
        }
    }
}