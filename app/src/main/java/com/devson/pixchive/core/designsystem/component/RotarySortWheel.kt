package com.devson.pixchive.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class SortField { TITLE, DATE, SIZE }
enum class SortDirection { ASCENDING, DESCENDING }

fun formatSortField(field: SortField): String {
    return when (field) {
        SortField.TITLE -> "Title"
        SortField.DATE -> "Date"
        SortField.SIZE -> "Size"
    }
}

fun getSortDirectionLabels(field: SortField): Pair<String, String> {
    return when (field) {
        SortField.TITLE -> "A → Z" to "Z → A"
        SortField.DATE -> "Oldest" to "Newest"
        SortField.SIZE -> "Smallest" to "Largest"
    }
}

fun parseSortOption(option: String): Pair<SortField, SortDirection> {
    return when (option.lowercase()) {
        "name_asc", "title_asc" -> SortField.TITLE to SortDirection.ASCENDING
        "name_desc", "title_desc" -> SortField.TITLE to SortDirection.DESCENDING
        "date_newest", "date_desc" -> SortField.DATE to SortDirection.DESCENDING
        "date_oldest", "date_asc" -> SortField.DATE to SortDirection.ASCENDING
        "size_asc" -> SortField.SIZE to SortDirection.ASCENDING
        "size_desc" -> SortField.SIZE to SortDirection.DESCENDING
        // Legacy options: gracefully fall back to date-newest
        "resolution_asc", "resolution_desc",
        "path_asc", "path_desc",
        "type_asc", "type_desc" -> SortField.DATE to SortDirection.DESCENDING
        else -> SortField.DATE to SortDirection.DESCENDING
    }
}

fun formatSortOption(field: SortField, direction: SortDirection): String {
    return when (field) {
        SortField.TITLE -> if (direction == SortDirection.ASCENDING) "name_asc" else "name_desc"
        SortField.DATE -> if (direction == SortDirection.DESCENDING) "date_newest" else "date_oldest"
        SortField.SIZE -> if (direction == SortDirection.ASCENDING) "size_asc" else "size_desc"
    }
}

@Composable
fun RotarySortWheelDialog(
    currentSortField: SortField,
    sortDirection: SortDirection,
    onSortFieldSelected: (SortField) -> Unit,
    onSortOrderToggled: (SortDirection) -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF000000).copy(alpha = 0.72f),
                            0.5f to Color(0xFF0A0E1A).copy(alpha = 0.82f),
                            1.0f to Color(0xFF000000).copy(alpha = 0.88f)
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header row: centred title chip + close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.width(48.dp)) // mirror of close button width
                        Spacer(Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.20f)
                            )
                        ) {
                            Text(
                                text = "Sort By",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    RotarySortWheel(
                        currentSortField = currentSortField,
                        sortDirection = sortDirection,
                        onSortFieldSelected = onSortFieldSelected,
                        onSortOrderToggled = onSortOrderToggled
                    )

                    // Bottom info card - Material You surface with strong contrast
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier.widthIn(min = 200.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp)
                        ) {
                            val dirLabels = getSortDirectionLabels(currentSortField)
                            Text(
                                text = formatSortField(currentSortField),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            // Direction pill
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.clickable {
                                    val nextDir = if (sortDirection == SortDirection.ASCENDING)
                                        SortDirection.DESCENDING else SortDirection.ASCENDING
                                    onSortOrderToggled(nextDir)
                                }
                            ) {
                                Text(
                                    text = if (sortDirection == SortDirection.ASCENDING)
                                        dirLabels.first else dirLabels.second,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RotarySortWheel(
    currentSortField: SortField,
    sortDirection: SortDirection,
    onSortFieldSelected: (SortField) -> Unit,
    onSortOrderToggled: (SortDirection) -> Unit
) {
    val items = remember { SortField.values() }
    val itemCount = items.size
    val initialSelectedIndex = items.indexOf(currentSortField).takeIf { it >= 0 } ?: 0
    val anglePerItem = 360f / itemCount

    val rotationAngle = remember { Animatable(-(initialSelectedIndex * anglePerItem)) }
    val coroutineScope = rememberCoroutineScope()

    // Sync wheel angle whenever the externally-committed sort field changes.
    LaunchedEffect(currentSortField) {
        val targetIndex = items.indexOf(currentSortField).takeIf { it >= 0 } ?: 0
        val currentRawIndex = Math.round(-rotationAngle.value / anglePerItem).toInt()
        val currentIndex = ((currentRawIndex % itemCount) + itemCount) % itemCount
        var diff = targetIndex - currentIndex
        if (diff > itemCount / 2) diff -= itemCount
        if (diff < -itemCount / 2) diff += itemCount
        val targetRawIndex = currentRawIndex + diff
        val targetAngle = -(targetRawIndex * anglePerItem)
        if (rotationAngle.value != targetAngle) {
            rotationAngle.animateTo(
                targetValue = targetAngle,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 180f)
            )
        }
    }

    // Wheel dimensions
    val wheelDiameterDp = 300.dp
    val radiusDp = 102.dp
    val density = LocalDensity.current
    val radiusPx = with(density) { radiusDp.toPx() }
    val centerHubRadiusPx = with(density) { 62.dp.toPx() }

    Box(
        modifier = Modifier
            .size(wheelDiameterDp)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = MaterialTheme.colorScheme.primary,
                spotColor = MaterialTheme.colorScheme.primary
            )
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                shape = CircleShape
            )
            .pointerInput(itemCount) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val distFromCenter = (down.position - center).getDistance()

                    // If user touches inside the center hub, let the buttons handle it directly
                    if (distFromCenter < centerHubRadiusPx) {
                        return@awaitEachGesture
                    }

                    var prevAngle = atan2(down.position.y - center.y, down.position.x - center.x)
                    var totalDragAngle = 0f

                    do {
                        val event = awaitPointerEvent()
                        val currentChange = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!currentChange.pressed) break

                        val currentPos = currentChange.position
                        val currAngle = atan2(currentPos.y - center.y, currentPos.x - center.x)

                        var angleDiff = Math.toDegrees((currAngle - prevAngle).toDouble()).toFloat()
                        if (angleDiff > 180f) angleDiff -= 360f
                        if (angleDiff < -180f) angleDiff += 360f

                        if (Math.abs(angleDiff) > 0.05f) {
                            totalDragAngle += Math.abs(angleDiff)
                            currentChange.consume()
                            coroutineScope.launch {
                                rotationAngle.snapTo(rotationAngle.value + angleDiff)
                            }
                            prevAngle = currAngle
                        }
                    } while (true)

                    // On drag release, snap to nearest item
                    if (totalDragAngle > 4f) {
                        val rawIndex = Math.round(-rotationAngle.value / anglePerItem).toInt()
                        var index = rawIndex % itemCount
                        if (index < 0) index += itemCount
                        val nextField = items[index]
                        onSortFieldSelected(nextField)
                        coroutineScope.launch {
                            rotationAngle.animateTo(
                                targetValue = -(rawIndex * anglePerItem),
                                animationSpec = spring(dampingRatio = 0.65f, stiffness = 180f)
                            )
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Top pointer indicator — triangle for clear directionality
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .size(width = 12.dp, height = 10.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 2.dp, topEnd = 2.dp,
                        bottomStart = 6.dp, bottomEnd = 6.dp
                    )
                )
                .background(MaterialTheme.colorScheme.primary)
        )

        // Orbit items
        items.forEachIndexed { index, field ->
            val angleDeg = (index * anglePerItem) + rotationAngle.value - 90f
            val angleRad = angleDeg * (PI.toFloat() / 180f)

            val xOffset = cos(angleRad) * radiusPx
            val yOffset = sin(angleRad) * radiusPx

            val isSelected = field == currentSortField

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.18f else 0.92f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f),
                label = "scale_$index"
            )

            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { xOffset.toDp() },
                        y = with(density) { yOffset.toDp() }
                    )
                    .scale(scale)
                    .defaultMinSize(minWidth = 60.dp, minHeight = 36.dp)
                    .then(
                        if (isSelected) {
                            Modifier
                                .shadow(6.dp, RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        } else {
                            Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(24.dp)
                                )
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onSortFieldSelected(field)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatSortField(field),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp
                )
            }
        }

        // Center hub — tonal surface for ASCENDING/DESCENDING toggle buttons
        Surface(
            modifier = Modifier.size(118.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                val dirLabels = getSortDirectionLabels(currentSortField)
                SortOrderButton(
                    label = dirLabels.first,
                    icon = Icons.Outlined.KeyboardArrowUp,
                    isActive = sortDirection == SortDirection.ASCENDING,
                    onClick = { onSortOrderToggled(SortDirection.ASCENDING) }
                )
                Spacer(modifier = Modifier.height(6.dp))
                SortOrderButton(
                    label = dirLabels.second,
                    icon = Icons.Outlined.KeyboardArrowDown,
                    isActive = sortDirection == SortDirection.DESCENDING,
                    onClick = { onSortOrderToggled(SortDirection.DESCENDING) }
                )
            }
        }
    }
}

@Composable
private fun SortOrderButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.4f,
        animationSpec = spring(),
        label = "alpha_$label"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (isActive)
                    Modifier.background(MaterialTheme.colorScheme.secondary)
                else
                    Modifier.background(
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.08f)
                    )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 7.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive)
                MaterialTheme.colorScheme.onSecondary
            else
                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = contentAlpha),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isActive)
                MaterialTheme.colorScheme.onSecondary
            else
                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = contentAlpha),
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}