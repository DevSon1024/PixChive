package com.devson.pixchive.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devson.pixchive.core.designsystem.component.CapsuleBottomBar

/**
 * Root App Shell for PixChive with responsive Scaffold and floating capsule navigation.
 */
@Composable
fun PixChiveApp(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Top-Level Tabs definition
    val tabs = listOf("Home", "Gallery", "Settings")

    // Determine if current screen is a top-level screen
    val selectedTabIndex = when {
        currentRoute?.contains("HomeDestination") == true -> 0
        currentRoute?.contains("GalleryDestination") == true -> 1
        currentRoute?.contains("SettingsDestination") == true -> 2
        else -> -1
    }

    val showBottomBar = selectedTabIndex != -1

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    CapsuleBottomBar(
                        tabs = tabs,
                        selectedTabIndex = if (selectedTabIndex >= 0) selectedTabIndex else 0,
                        onTabSelected = { index ->
                            val targetDestination = when (index) {
                                0 -> HomeDestination
                                1 -> GalleryDestination
                                2 -> SettingsDestination
                                else -> HomeDestination
                            }

                            if (selectedTabIndex != index) {
                                navController.navigate(targetDestination) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // NavHost occupies full screen for edge-to-edge drawing; screens handle their own insets
        NavGraph(
            navController = navController,
            modifier = Modifier.fillMaxSize()
        )
    }
}
