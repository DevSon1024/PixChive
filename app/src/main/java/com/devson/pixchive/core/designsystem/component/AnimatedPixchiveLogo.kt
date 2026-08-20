package com.devson.pixchive.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Geometry Data Models
// ---------------------------------------------------------------------------

private data class PctLine(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

// ---------------------------------------------------------------------------
// Grid Coordinates (Clean crosshatch matching PixChive branding)
// ---------------------------------------------------------------------------

private val PixchiveGridLines = listOf(
    // 3 Horizontal crosshatch lines
    PctLine(49f, 62f, 76f, 62f),
    PctLine(48f, 67f, 75f, 67f),
    PctLine(47f, 72f, 74f, 72f),
    // 5 Vertical crosshatch lines
    PctLine(52f, 58f, 51f, 76f),
    PctLine(57f, 58f, 56f, 76f),
    PctLine(62f, 58f, 61f, 76f),
    PctLine(67f, 58f, 66f, 76f),
    PctLine(72f, 58f, 71f, 76f)
)

// ---------------------------------------------------------------------------
// Path Builders (Precise cubic/quadratic bezier curves)
// ---------------------------------------------------------------------------

/**
 * Builds the folder silhouette matching the reference artwork:
 * Tab on top-left, flowing down diagonally to the main body, rounded corners,
 * and slightly tapered sides.
 */
private fun buildFolderContour(w: Float, h: Float, cornerRadius: Float): Path {
    val path = Path()
    fun x(pct: Float) = pct / 100f * w
    fun y(pct: Float) = pct / 100f * h
    val cr = cornerRadius

    // Start at top tab (after top-left corner curve)
    path.moveTo(x(18f) + cr, y(44f))
    // Tab horizontal top edge
    path.lineTo(x(39f), y(44f))
    // Smooth diagonal slope down to main top edge
    path.cubicTo(
        x(42.5f), y(44f),
        x(44.5f), y(49.5f),
        x(49f), y(49.5f)
    )
    // Main top edge to top-right corner
    path.lineTo(x(82f) - cr, y(49.5f))
    // Top-right rounded corner
    path.quadraticTo(x(84.5f), y(49.5f), x(84f), y(49.5f) + cr)
    // Right edge (gentle natural taper)
    path.lineTo(x(80.5f), y(83.5f) - cr)
    // Bottom-right rounded corner
    path.quadraticTo(x(80.5f), y(85f), x(80.5f) - cr * 1.2f, y(85f))
    // Bottom horizontal edge
    path.lineTo(x(21.5f) + cr * 1.2f, y(85f))
    // Bottom-left rounded corner
    path.quadraticTo(x(16.5f), y(85f), x(16.5f), y(83.5f) - cr)
    // Left edge (gentle natural taper)
    path.lineTo(x(18f), y(44f) + cr)
    // Top-left tab corner
    path.quadraticTo(x(18f), y(44f), x(18f) + cr, y(44f))
    path.close()
    return path
}

/**
 * Builds the small folder back-tab visible behind the right card.
 */
private fun buildFolderBackFlap(w: Float, h: Float, cornerRadius: Float): Path {
    val path = Path()
    fun x(pct: Float) = pct / 100f * w
    fun y(pct: Float) = pct / 100f * h
    val cr = cornerRadius * 0.6f

    path.moveTo(x(76f), y(48f))
    path.lineTo(x(76f), y(43.5f) + cr)
    path.quadraticTo(x(76f), y(43f), x(76f) + cr, y(43f))
    path.lineTo(x(80f) - cr, y(43f))
    path.quadraticTo(x(80f), y(43f), x(80f), y(43.5f) + cr)
    path.lineTo(x(80f), y(48f))
    return path
}

/**
 * Builds the mountain landscape peaks inside the center photo card.
 */
private fun buildMountainPath(w: Float, h: Float): Path {
    val path = Path()
    fun x(pct: Float) = pct / 100f * w
    fun y(pct: Float) = pct / 100f * h

    // Left base -> High Peak -> Valley -> Secondary Peak -> Right base
    path.moveTo(x(-10f), y(8f))
    path.lineTo(x(-3.5f), y(-6.5f))
    path.lineTo(x(1.5f), y(2.5f))
    path.lineTo(x(6f), y(-2.5f))
    path.lineTo(x(10.5f), y(6.5f))
    return path
}

// ---------------------------------------------------------------------------
// Animation & Trimming Helpers
// ---------------------------------------------------------------------------

private fun trimmedSegment(path: Path, progress: Float): Path {
    val out = Path()
    if (progress <= 0f) return out
    val measure = PathMeasure().apply { setPath(path, false) }
    measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), out, true)
    return out
}

private fun tipPosition(path: Path, progress: Float): Offset? {
    if (progress <= 0f || progress >= 1f) return null
    val measure = PathMeasure().apply { setPath(path, false) }
    return measure.getPosition(measure.length * progress)
}

private fun stageProgress(overall: Float, start: Float, end: Float): Float =
    ((overall - start) / (end - start)).coerceIn(0f, 1f)

private fun smoothStep(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun DrawScope.drawTipGlow(p: Offset, color: Color, glowRadiusPx: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.85f), Color.Transparent),
            center = p,
            radius = glowRadiusPx * 2.2f
        ),
        radius = glowRadiusPx * 2.2f,
        center = p
    )
    drawCircle(color = color, radius = glowRadiusPx * 0.5f, center = p)
}

/**
 * Draws a framed photo card with outer rounded border and inner picture frame.
 */
private fun DrawScope.drawFramedCard(
    center: Offset,
    width: Float,
    height: Float,
    rotationDegrees: Float,
    cornerRadiusPx: Float,
    outerProgress: Float,
    innerProgress: Float,
    strokeStyle: Stroke,
    color: Color,
    maskColor: Color,
    glowRadiusPx: Float
) {
    if (outerProgress <= 0f) return
    rotate(rotationDegrees, pivot = center) {
        val outerPath = Path().apply {
            addRoundRect(
                RoundRect(
                    Rect(center - Offset(width / 2f, height / 2f), Size(width, height)),
                    CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
            )
        }
        val fillAlpha = (outerProgress * 3f).coerceIn(0f, 1f)
        drawPath(outerPath, color = maskColor.copy(alpha = maskColor.alpha * fillAlpha))
        drawPath(trimmedSegment(outerPath, outerProgress), color = color, style = strokeStyle)
        tipPosition(outerPath, outerProgress)?.let { drawTipGlow(it, color, glowRadiusPx) }

        if (innerProgress > 0f) {
            val innerW = width * 0.76f
            val innerH = height * 0.78f
            val innerCr = cornerRadiusPx * 0.65f
            val innerPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        Rect(center - Offset(innerW / 2f, innerH / 2f), Size(innerW, innerH)),
                        CornerRadius(innerCr, innerCr)
                    )
                )
            }
            drawPath(trimmedSegment(innerPath, innerProgress), color = color, style = strokeStyle)
            tipPosition(innerPath, innerProgress)?.let { drawTipGlow(it, color, glowRadiusPx) }
        }
    }
}

// ---------------------------------------------------------------------------
// Main Animated Logo Composable
// ---------------------------------------------------------------------------

@Composable
fun AnimatedPixchiveLogo(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    maskColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    animateOnEntry: Boolean = true
) {
    var hasAnimated by rememberSaveable { mutableStateOf(!animateOnEntry) }

    val containerAlpha = remember { Animatable(if (hasAnimated) 1f else 0f) }
    val containerScale = remember { Animatable(if (hasAnimated) 1f else 0.85f) }
    val drawProgress = remember { Animatable(if (hasAnimated) 1f else 0f) }

    LaunchedEffect(hasAnimated) {
        if (!hasAnimated) {
            launch { containerAlpha.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
            launch {
                containerScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            delay(80)
            drawProgress.animateTo(1f, tween(durationMillis = 1400, easing = FastOutSlowInEasing))
            // Gentle settle bounce
            containerScale.animateTo(1.04f, tween(120, easing = FastOutSlowInEasing))
            containerScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            hasAnimated = true
        }
    }

    val idleTransition = rememberInfiniteTransition(label = "pixchive_idle")
    val breathe by idleTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    Box(
        modifier = modifier
            .scale(containerScale.value * breathe)
            .alpha(containerAlpha.value),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)

            // Dynamic stroke proportional to canvas size
            val strokeWidthPx = minDim * 0.044f
            val strokeStyle = Stroke(strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val glowRadiusPx = minDim * 0.055f
            val overall = drawProgress.value

            // Animation stage progress mappings
            val backFlapP = stageProgress(overall, 0f, 0.15f)
            val leftCardOuterP = stageProgress(overall, 0.05f, 0.28f)
            val leftCardInnerP = stageProgress(overall, 0.20f, 0.35f)
            val rightCardOuterP = stageProgress(overall, 0.12f, 0.34f)
            val rightCardInnerP = stageProgress(overall, 0.26f, 0.40f)

            val centerCardOuterP = stageProgress(overall, 0.30f, 0.52f)
            val centerCardInnerP = stageProgress(overall, 0.46f, 0.62f)
            val mountainP = stageProgress(overall, 0.56f, 0.70f)
            val sunCircleP = stageProgress(overall, 0.66f, 0.76f)

            val folderP = stageProgress(overall, 0.64f, 0.88f)
            val gridP = stageProgress(overall, 0.82f, 1.0f)

            // Geometry centers & dimensions
            val cardW = 26f / 100f * w
            val cardH = 37f / 100f * h
            val cardCr = minDim * 0.045f

            val centerCardW = 32f / 100f * w
            val centerCardH = 45f / 100f * h
            val centerCardCr = minDim * 0.05f

            val leftCardCenter = Offset(33.5f / 100f * w, 37f / 100f * h)
            val rightCardCenter = Offset(66.5f / 100f * w, 39f / 100f * h)
            val centerCardCenter = Offset(50f / 100f * w, 31f / 100f * h)

            // 1. Back flap (behind right card)
            if (backFlapP > 0f) {
                val backFlapPath = buildFolderBackFlap(w, h, minDim * 0.04f)
                drawPath(trimmedSegment(backFlapPath, backFlapP), color = color, style = strokeStyle)
            }

            // 2. Left Photo Card (tilted -17°)
            drawFramedCard(
                center = leftCardCenter,
                width = cardW,
                height = cardH,
                rotationDegrees = -17f,
                cornerRadiusPx = cardCr,
                outerProgress = leftCardOuterP,
                innerProgress = leftCardInnerP,
                strokeStyle = strokeStyle,
                color = color,
                maskColor = maskColor,
                glowRadiusPx = glowRadiusPx
            )

            // 3. Right Photo Card (tilted +17°)
            drawFramedCard(
                center = rightCardCenter,
                width = cardW,
                height = cardH,
                rotationDegrees = 17f,
                cornerRadiusPx = cardCr,
                outerProgress = rightCardOuterP,
                innerProgress = rightCardInnerP,
                strokeStyle = strokeStyle,
                color = color,
                maskColor = maskColor,
                glowRadiusPx = glowRadiusPx
            )

            // 4. Center Hero Photo Card (tilted -7° with mountain & sun)
            if (centerCardOuterP > 0f) {
                rotate(-7f, pivot = centerCardCenter) {
                    val outerCardPath = Path().apply {
                        addRoundRect(
                            RoundRect(
                                Rect(
                                    centerCardCenter - Offset(centerCardW / 2f, centerCardH / 2f),
                                    Size(centerCardW, centerCardH)
                                ),
                                CornerRadius(centerCardCr, centerCardCr)
                            )
                        )
                    }
                    val fillAlpha = (centerCardOuterP * 3f).coerceIn(0f, 1f)
                    drawPath(outerCardPath, color = maskColor.copy(alpha = maskColor.alpha * fillAlpha))
                    drawPath(trimmedSegment(outerCardPath, centerCardOuterP), color = color, style = strokeStyle)
                    tipPosition(outerCardPath, centerCardOuterP)?.let { drawTipGlow(it, color, glowRadiusPx) }

                    // Inner border frame
                    if (centerCardInnerP > 0f) {
                        val innerW = centerCardW * 0.77f
                        val innerH = centerCardH * 0.80f
                        val innerCr = centerCardCr * 0.65f
                        val innerPath = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    Rect(
                                        centerCardCenter - Offset(innerW / 2f, innerH / 2f),
                                        Size(innerW, innerH)
                                    ),
                                    CornerRadius(innerCr, innerCr)
                                )
                            )
                        }
                        drawPath(trimmedSegment(innerPath, centerCardInnerP), color = color, style = strokeStyle)
                        tipPosition(innerPath, centerCardInnerP)?.let { drawTipGlow(it, color, glowRadiusPx) }
                    }

                    // Mountain glyph inside center card
                    if (mountainP > 0f) {
                        val mountainPath = Path().apply {
                            val m = buildMountainPath(w, h)
                            // Translate to center card center
                            addPath(m, centerCardCenter)
                        }
                        drawPath(trimmedSegment(mountainPath, mountainP), color = color, style = strokeStyle)
                        tipPosition(mountainPath, mountainP)?.let { drawTipGlow(it, color, glowRadiusPx) }
                    }

                    // Sun circle inside center card
                    if (sunCircleP > 0f) {
                        val sunCenter = centerCardCenter + Offset(5.8f / 100f * w, -8.5f / 100f * h)
                        val sunRadius = 2.4f / 100f * minDim
                        val eased = smoothStep(sunCircleP)
                        drawCircle(
                            color = color.copy(alpha = color.alpha * eased),
                            radius = sunRadius * eased,
                            center = sunCenter,
                            style = strokeStyle
                        )
                    }
                }
            }

            // 5. Folder Silhouette (covers the bottom half of the cards)
            if (folderP > 0f) {
                val folderPath = buildFolderContour(w, h, minDim * 0.055f)
                val fillAlpha = (folderP * 3f).coerceIn(0f, 1f)
                drawPath(folderPath, color = maskColor.copy(alpha = maskColor.alpha * fillAlpha))
                drawPath(trimmedSegment(folderPath, folderP), color = color, style = strokeStyle)
                tipPosition(folderPath, folderP)?.let { drawTipGlow(it, color, glowRadiusPx) }
            }

            // 6. Crosshatch Thumbnail Grid on Folder Face
            PixchiveGridLines.forEachIndexed { index, line ->
                val delay = index * 0.06f
                val local = ((gridP - delay) / (1f - delay)).coerceIn(0f, 1f)
                if (local > 0f) {
                    val linePath = Path().apply {
                        moveTo(line.x1 / 100f * w, line.y1 / 100f * h)
                        lineTo(line.x2 / 100f * w, line.y2 / 100f * h)
                    }
                    drawPath(
                        trimmedSegment(linePath, local),
                        color = color,
                        style = Stroke(strokeWidthPx * 0.88f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}
