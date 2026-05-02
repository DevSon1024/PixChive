package com.devson.pixchive.gallery.ui.components

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import kotlinx.coroutines.launch

/**
 * A custom clickable modifier that DOES NOT consume the down pointer event on Main pass,
 * which allows the parent PinchZoomGrid to also receive it for pinch detection.
 *
 * FIX: Before handling any gesture, we verify the DOWN landed within this composable's
 * own bounds. Because [requireUnconsumed = false] means every sibling item receives the
 * same down event, without this check every item in the grid would handle every touch.
 */
fun Modifier.galleryItemClick(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null
): Modifier = composed {
    val currentInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val currentIndication = indication ?: LocalIndication.current
    val viewConfiguration = LocalViewConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    this
        .indication(currentInteractionSource, currentIndication)
        .pointerInput(currentInteractionSource) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)

                // BOUNDS CHECK 
                // Since requireUnconsumed=false, ALL sibling items receive this
                // down event. Only the item whose bounds contain the touch point
                // should proceed; all others must bail out here.
                val downPos = down.position
                if (downPos.x < 0f || downPos.x > size.width ||
                    downPos.y < 0f || downPos.y > size.height
                ) {
                    return@awaitEachGesture
                }

                val press = PressInteraction.Press(downPos)
                coroutineScope.launch { currentInteractionSource.emit(press) }

                var isLongPress = false
                var upOrCancelEvent: androidx.compose.ui.input.pointer.PointerEvent? = null
                val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                val touchSlop = viewConfiguration.touchSlop
                val initialPosition = downPos

                try {
                    withTimeout(longPressTimeout) {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            if (event.changes.any { it.isConsumed }) break
                            if (event.changes.size > 1) break  // pinch → hand off

                            val pos = event.changes.first().position
                            // Left bounds via slop
                            val distance = (pos - initialPosition).getDistance()
                            if (distance > touchSlop) break

                            if (event.changes.all { !it.pressed }) {
                                upOrCancelEvent = event
                                break
                            }
                        }
                    }
                } catch (e: PointerEventTimeoutCancellationException) {
                    isLongPress = true
                }

                if (isLongPress) {
                    coroutineScope.launch {
                        currentInteractionSource.emit(PressInteraction.Cancel(press))
                    }
                    onLongClick?.invoke()
                    // Drain & consume ALL remaining pointer events until finger lifts
                    // so the UP cannot propagate to any sibling or parent and trigger
                    // an accidental click on the item below.
                    var drainEvent: androidx.compose.ui.input.pointer.PointerEvent
                    do {
                        drainEvent = awaitPointerEvent(PointerEventPass.Initial)
                        drainEvent.changes.forEach { it.consume() }
                    } while (drainEvent.changes.any { it.pressed })
                } else if (upOrCancelEvent != null) {
                    coroutineScope.launch {
                        currentInteractionSource.emit(PressInteraction.Release(press))
                    }
                    upOrCancelEvent!!.changes.forEach { it.consume() }
                    onClick()
                } else {
                    coroutineScope.launch {
                        currentInteractionSource.emit(PressInteraction.Cancel(press))
                    }
                }
            }
        }
}
