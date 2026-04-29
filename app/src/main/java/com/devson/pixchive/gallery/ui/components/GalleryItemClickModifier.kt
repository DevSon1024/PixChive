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
 * A custom clickable modifier that DOES NOT consume the down pointer event.
 * This allows parent gesture detectors (like PinchZoomGrid) to still process
 * the touch events, particularly the initial down event and multi-touch gestures.
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
                // Do not consume the down event to let PinchZoomGrid handle it!
                
                val press = PressInteraction.Press(down.position)
                coroutineScope.launch {
                    currentInteractionSource.emit(press)
                }

                var isLongPress = false
                var upOrCancelEvent: androidx.compose.ui.input.pointer.PointerEvent? = null
                val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                val touchSlop = viewConfiguration.touchSlop
                val initialPosition = down.position

                try {
                    withTimeout(longPressTimeout) {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            if (event.changes.any { it.isConsumed }) {
                                // Event was consumed by something else (e.g. scroll)
                                break
                            }
                            if (event.changes.size > 1) {
                                // Multi-touch detected, probably a pinch
                                break
                            }
                            // Check if pointer went out of bounds (slop)
                            val pos = event.changes.first().position
                            if (pos.x < 0 || pos.x > size.width || pos.y < 0 || pos.y > size.height) {
                                break
                            }
                            
                            // Check if pointer moved beyond touch slop
                            val distance = (pos - initialPosition).getDistance()
                            if (distance > touchSlop) {
                                break
                            }
                            
                            if (event.changes.all { !it.pressed }) {
                                upOrCancelEvent = event
                                break
                            }
                        }
                    }
                } catch (e: PointerEventTimeoutCancellationException) {
                    // Timeout -> Long Press
                    isLongPress = true
                }

                if (isLongPress) {
                    coroutineScope.launch {
                        currentInteractionSource.emit(PressInteraction.Cancel(press))
                    }
                    onLongClick?.invoke()
                    // Wait for the pointer to go up
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        if (event.changes.all { !it.pressed }) break
                    }
                } else if (upOrCancelEvent != null) {
                    coroutineScope.launch {
                        currentInteractionSource.emit(PressInteraction.Release(press))
                    }
                    // Consume the up event so it counts as a click
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
