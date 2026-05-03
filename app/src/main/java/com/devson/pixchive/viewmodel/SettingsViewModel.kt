package com.devson.pixchive.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.pixchive.data.PreferencesManager
import com.devson.pixchive.ui.theme.AppThemePalette
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager(application)

    val isDarkTheme: StateFlow<Boolean?> = prefs.appThemeFlow.map {
        when (it) {
            "light" -> false
            "dark" -> true
            else -> null
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val dynamicColor: StateFlow<Boolean> = prefs.dynamicColorFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val selectedPalette: StateFlow<AppThemePalette> = prefs.selectedPaletteFlow.map { name ->
        try {
            AppThemePalette.valueOf(name)
        } catch (e: Exception) {
            AppThemePalette.BLUE
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, AppThemePalette.BLUE)

    val isNavBarTransparent: StateFlow<Boolean> = prefs.navBarTransparentFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val isBackgroundBlurEnabled: StateFlow<Boolean> = prefs.isBackgroundBlurEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            prefs.saveAppTheme(if (isDark) "dark" else "light")
        }
    }

    fun resetDarkTheme() {
        viewModelScope.launch {
            prefs.saveAppTheme("system")
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            prefs.saveDynamicColor(enabled)
        }
    }

    fun setSelectedPalette(palette: AppThemePalette) {
        viewModelScope.launch {
            prefs.saveSelectedPalette(palette.name)
        }
    }

    fun setNavBarTransparent(transparent: Boolean) {
        viewModelScope.launch {
            prefs.saveNavBarTransparent(transparent)
        }
    }

    fun setBackgroundBlurEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setBackgroundBlurEnabled(enabled)
        }
    }
}
