package com.devson.pixchive.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devson.pixchive.core.designsystem.theme.PixchiveTheme

/**
 * Data model for capsule bottom navigation tab items.
 */
data class NavTabItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Data model for selection action items displayed inside the capsule during selection mode.
 */
data class SelectionActionItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isDestructive: Boolean = false
)

/**
 * Modern floating Material 3 capsule bottom bar that seamlessly transitions between 
 * navigation tabs and selection mode action buttons.
 */
@Composable
fun CapsuleBottomBar(
    items: List<NavTabItem>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isSelectionActive: Boolean = false,
    selectionActions: List<SelectionActionItem> = emptyList()
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .padding(bottom = 16.dp)
            .height(58.dp)
            .wrapContentWidth()
            .graphicsLayer {
                shadowElevation = 10.dp.toPx()
                shape = RoundedCornerShape(30.dp)
                clip = true
            },
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 6.dp
    ) {
        AnimatedContent(
            targetState = isSelectionActive && selectionActions.isNotEmpty(),
            transitionSpec = {
                fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                        expandHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                        shrinkHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            },
            label = "capsule_mode_transition"
        ) { isSelection ->
            if (isSelection) {
                // Selection Mode Action Buttons
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    selectionActions.forEach { action ->
                        val contentColor = if (action.isDestructive) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                action.onClick()
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Transparent
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = action.label,
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = action.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
            } else {
                // Default Navigation Tabs
                Row(
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = selectedTabIndex == index

                        val backgroundColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                            label = "tab_bg"
                        )

                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "tab_content"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(24.dp))
                                .background(backgroundColor)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (!isSelected) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onTabSelected(index)
                                    }
                                }
                                .padding(horizontal = if (isSelected) 16.dp else 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )

                            AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                        expandHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)),
                                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessHigh)) +
                                        shrinkHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh))
                            ) {
                                Row {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.label,
                                        color = contentColor,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Text-only overload for simple tab lists.
 */
@JvmName("CapsuleBottomBarTabs")
@Composable
fun CapsuleBottomBar(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .padding(bottom = 16.dp)
            .height(54.dp)
            .wrapContentWidth()
            .graphicsLayer {
                shadowElevation = 8.dp.toPx()
                shape = RoundedCornerShape(28.dp)
                clip = true
            },
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "bg_color"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "content_color"
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(backgroundColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTabSelected(index)
                            }
                        }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = contentColor,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Alias for backward compatibility.
 */
@Composable
fun CapsuleBottomNavigation(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    CapsuleBottomBar(
        tabs = tabs,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun CapsuleBottomBarPreview() {
    PixchiveTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CapsuleBottomBar(
                items = listOf(
                    NavTabItem("Home", Icons.Filled.Home, Icons.Outlined.Home),
                    NavTabItem("Gallery", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
                    NavTabItem("Library", Icons.Filled.CollectionsBookmark, Icons.Outlined.CollectionsBookmark),
                    NavTabItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
                ),
                selectedTabIndex = 0,
                onTabSelected = {}
            )
        }
    }
}
