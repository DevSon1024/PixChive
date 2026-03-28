package com.devson.pixchive.ui.reader.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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

    // Smooth color transition - active buttons glow with primary color
    val backgroundColor = animateColorAsState(
        targetValue = when {
            isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
            isPressed -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        },
        animationSpec = tween(200),
        label = "backgroundColor"
    ).value

    val contentColor = animateColorAsState(
        targetValue = if (isActive)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.onPrimaryContainer,
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