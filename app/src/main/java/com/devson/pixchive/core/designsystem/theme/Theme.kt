package com.devson.pixchive.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val NosvedShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun PixchiveTheme(
    forceDark: Boolean? = null,
    dynamicColor: Boolean = false,
    palette: AppThemePalette = AppThemePalette.CINEMATIC,
    isAmoled: Boolean = false,
    isNavBarTransparent: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme  = forceDark ?: systemDark

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            val dynamicScheme = if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            if (darkTheme && isAmoled) {
                dynamicScheme.copy(
                    background = AmoledBackground,
                    surface = AmoledSurface,
                    surfaceVariant = AmoledSurfaceContainer,
                    surfaceContainerLowest = AmoledBackground,
                    surfaceContainerLow = AmoledSurface,
                    surfaceContainer = AmoledSurfaceContainer
                )
            } else {
                dynamicScheme
            }
        }
        darkTheme -> palette.darkScheme(isAmoled = isAmoled)
        else      -> palette.lightScheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                WindowCompat.setDecorFitsSystemWindows(window, false)
                @Suppress("DEPRECATION")
                window.statusBarColor = Color.Transparent.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = if (isNavBarTransparent)
                    Color.Transparent.toArgb()
                else
                    colorScheme.background.toArgb()
                insetsController.isAppearanceLightStatusBars     = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = NosvedShapes,
        content     = content
    )
}

@Composable
fun DialogNavigationBarThemeFix() {
    val view      = LocalView.current
    val darkTheme = isSystemInDarkTheme()
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.navigationBarColor = Color.Transparent.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
}

@Preview(name = "Light Theme Preview", showBackground = true)
@Composable
private fun PixchiveThemeLightPreview() {
    PixchiveTheme(forceDark = false) {
        Surface {
            Text(text = "PixChive Light Theme", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Preview(name = "Dark Theme Preview", showBackground = true)
@Composable
private fun PixchiveThemeDarkPreview() {
    PixchiveTheme(forceDark = true) {
        Surface {
            Text(text = "PixChive Dark Theme", style = MaterialTheme.typography.titleLarge)
        }
    }
}