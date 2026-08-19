package com.devson.pixchive.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import kotlinx.coroutines.launch

/**
 * Clickable modifier that applies a bouncy scale compression and tactile haptic feedback.
 */
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    targetScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .clickable(
            enabled = enabled,
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                scale.animateTo(
                    targetValue = targetScale,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                )
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                )
            }
            onClick()
        }
}

/**
 * Standard clickable modifier augmented with native haptic feedback.
 */
fun Modifier.hapticClickable(
    enabled: Boolean = true,
    hapticType: HapticFeedbackType = HapticFeedbackType.LongPress,
    onClick: () -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    this.clickable(enabled = enabled) {
        haptic.performHapticFeedback(hapticType)
        onClick()
    }
}

/**
 * A custom clickable modifier that DOES NOT consume the down pointer event on Main pass,
 * which allows the parent PinchZoomGrid to also receive it for pinch detection.
 * If isSelectionModeActive is true, single taps automatically route to onToggleSelection or onClick.
 */
fun Modifier.galleryItemClick(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isSelectionModeActive: Boolean = false,
    onToggleSelection: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null
): Modifier = composed {
    val currentInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val currentIndication = indication ?: LocalIndication.current
    val viewConfiguration = LocalViewConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    this
        .indication(currentInteractionSource, currentIndication)
        .pointerInput(currentInteractionSource, isSelectionModeActive) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)

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
                    if (isSelectionModeActive && onToggleSelection != null) {
                        onToggleSelection.invoke()
                    } else {
                        onLongClick?.invoke()
                    }
                    // Drain and consume remaining events until lift
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
                    if (isSelectionModeActive && onToggleSelection != null) {
                        onToggleSelection()
                    } else {
                        onClick()
                    }
                } else {
                    coroutineScope.launch {
                        currentInteractionSource.emit(PressInteraction.Cancel(press))
                    }
                }
            }
        }
}
