package com.devson.pixchive.feature.gallery.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devson.pixchive.core.designsystem.component.CapsuleBottomBar

/**
 * Delegated to core design system CapsuleBottomBar.
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