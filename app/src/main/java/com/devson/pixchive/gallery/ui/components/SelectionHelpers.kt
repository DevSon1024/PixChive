package com.devson.pixchive.gallery.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.SelectionCheckmarkOverlay(visible: Boolean = true) {
    val scale by animateFloatAsState(
        targetValue  = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "checkScale"
    )
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp)
            .scale(scale)
            .size(24.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector  = Icons.Filled.Check,
            contentDescription = "Selected",
            tint         = MaterialTheme.colorScheme.onPrimary,
            modifier     = Modifier.size(14.dp)
        )
    }
}

@Composable
fun SelectionScrimOverlay(visible: Boolean) {
    if (visible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.2f))
        )
    }
}

fun Modifier.gridDragSelect(
    state: LazyGridState,
    onSelectionChange: (Int, Int) -> Unit,
    onDragStart: (Int) -> Unit = {},
    onDragEnd: () -> Unit = {}
): Modifier = this.then(
    Modifier.pointerInput(state) {
        var initialIndex: Int? = null
        var currentIndex: Int? = null

        detectDragGesturesAfterLongPress(
            onDragStart = { offset ->
                val viewportOffset = state.layoutInfo.viewportStartOffset
                val contentOffset = offset.copy(y = offset.y - viewportOffset)

                val item = state.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                    val rect = Rect(
                        item.offset.x.toFloat(),
                        item.offset.y.toFloat(),
                        item.offset.x.toFloat() + item.size.width,
                        item.offset.y.toFloat() + item.size.height
                    )
                    rect.contains(contentOffset)
                }
                item?.let {
                    initialIndex = it.index
                    currentIndex = it.index
                    onDragStart(it.index)
                }
            },
            onDrag = { change, _ ->
                val viewportOffset = state.layoutInfo.viewportStartOffset
                val contentOffset = change.position.copy(y = change.position.y - viewportOffset)

                val item = state.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                    val rect = Rect(
                        item.offset.x.toFloat(),
                        item.offset.y.toFloat(),
                        item.offset.x.toFloat() + item.size.width,
                        item.offset.y.toFloat() + item.size.height
                    )
                    rect.contains(contentOffset)
                }
                item?.let {
                    if (it.index != currentIndex) {
                        currentIndex = it.index
                        initialIndex?.let { start ->
                            onSelectionChange(start, it.index)
                        }
                    }
                }
            },
            onDragEnd = {
                initialIndex = null
                currentIndex = null
                onDragEnd()
            },
            onDragCancel = {
                initialIndex = null
                currentIndex = null
                onDragEnd()
            }
        )
    }
)
