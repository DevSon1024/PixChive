package com.devson.pixchive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.devson.pixchive.data.PreferencesManager
import com.devson.pixchive.navigation.NavGraph
import com.devson.pixchive.ui.theme.AppThemePalette
import com.devson.pixchive.ui.theme.PixchiveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val preferencesManager = remember { PreferencesManager(context) }
            val appTheme by preferencesManager.appThemeFlow.collectAsState(initial = "system")
            val dynamicColor by preferencesManager.dynamicColorFlow.collectAsState(initial = true)
            val selectedPaletteStr by preferencesManager.selectedPaletteFlow.collectAsState(initial = "BLUE")
            val selectedPalette = try { AppThemePalette.valueOf(selectedPaletteStr) } catch(e: Exception) { AppThemePalette.BLUE }
            val isNavBarTransparent by preferencesManager.navBarTransparentFlow.collectAsState(initial = false)

            // Determine if dark theme should be enabled based on preference
            val isDarkTheme = when (appTheme) {
                "light" -> false
                "dark" -> true
                else -> null
            }

            PixchiveTheme(
                forceDark = isDarkTheme,
                dynamicColor = dynamicColor,
                palette = selectedPalette,
                isNavBarTransparent = isNavBarTransparent
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
}