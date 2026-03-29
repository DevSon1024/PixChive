package com.devson.pixchive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import com.devson.pixchive.data.PreferencesManager
import com.devson.pixchive.utils.BackupRestoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToDeveloperOptions: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }

    val devOptionsEnabled by preferencesManager.developerOptionsEnabledFlow.collectAsState(initial = false)
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    val showHiddenFiles by preferencesManager.showHiddenFilesFlow.collectAsState(initial = false)
    val appTheme by preferencesManager.appThemeFlow.collectAsState(initial = "system")

    val volumeKeysNavigation by preferencesManager.volumeKeysNavigationFlow.collectAsState(initial = true)

    // Resolve version name
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val success = BackupRestoreManager.performBackup(context, uri)
                launch(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (success) "Backup created successfully" else "Failed to create backup",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val success = BackupRestoreManager.performRestore(context, uri)
                launch(Dispatchers.Main) {
                    if (!success) {
                        Toast.makeText(context, "Failed to restore backup", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            //  App identity card (Google-Play-style header) 
            // App identity card (Google-Play-style header) 
            AppIdentityCard(
                versionName = versionName,
                onVersionTap = {
                    val now = System.currentTimeMillis()
                    // Reset tap count if user pauses for more than 500ms
                    if (now - lastTapTime > 500) tapCount = 1 else tapCount++
                    lastTapTime = now

                    if (!devOptionsEnabled) {
                        if (tapCount >= 8) {
                            scope.launch { preferencesManager.setDeveloperOptionsEnabled(true) }
                            Toast.makeText(context, "You are now a developer!", Toast.LENGTH_SHORT).show()
                            tapCount = 0
                        } else if (tapCount >= 4) {
                            Toast.makeText(context, "Tap ${8 - tapCount} more times to be a developer", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            Spacer(Modifier.height(20.dp))

            //  Appearance 
            SettingsSectionLabel("Appearance")
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.Palette,
                    title = "Theme",
                    subtitle = when (appTheme) {
                        "light" -> "Light"
                        "dark" -> "Dark"
                        else -> "System Default"
                    },
                    onClick = onNavigateToAppearance
                )
            }

            Spacer(Modifier.height(16.dp))

            //  General 
            SettingsSectionLabel("General")
            SettingsCard {
                SettingsToggleRow(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Volume button use",
                    subtitle = "Use volume buttons to navigate pages",
                    checked = volumeKeysNavigation,
                    onCheckedChange = { checked ->
                        scope.launch { preferencesManager.setVolumeKeysNavigation(checked) }
                    }
                )
                SettingsDivider()
                SettingsToggleRow(
                    icon = Icons.Default.FolderOpen,
                    title = "Show hidden files",
                    subtitle = "Include folders and files starting with a dot (.)",
                    checked = showHiddenFiles,
                    onCheckedChange = { checked ->
                        scope.launch { preferencesManager.setShowHiddenFiles(checked) }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            //  Data & Backup 
            SettingsSectionLabel("Data & Backup")
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.Backup,
                    title = "Create Backup",
                    subtitle = "Export database and settings to a zip file",
                    onClick = {
                        backupLauncher.launch("PixChive_Backup_${System.currentTimeMillis()}.zip")
                    }
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Restore,
                    title = "Restore Backup",
                    subtitle = "Import data and settings from a previous backup",
                    onClick = {
                        restoreLauncher.launch(arrayOf("application/zip"))
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            //  Privacy 
            SettingsSectionLabel("Privacy")
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.Lock,
                    title = "Privacy & Policy",
                    subtitle = "User data privacy, offline details",
                    onClick = onNavigateToPrivacyPolicy
                )
            }

            Spacer(Modifier.height(16.dp))

            // --- ADD DEVELOPER SECTION HERE ---
            if (devOptionsEnabled) {
                SettingsSectionLabel("Developer")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.Code,
                        title = "Developer Options",
                        subtitle = "Logs, debugging, and experimental features",
                        onClick = onNavigateToDeveloperOptions
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            //  About 
            SettingsSectionLabel("About")
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.Info,
                    title = "About PixChive",
                    subtitle = "Libraries, version info, and developer links",
                    onClick = onNavigateToAbout
                )
            }
        }
    }
}

//  App Identity Header Card 

@Composable
private fun AppIdentityCard(versionName: String, onVersionTap: () -> Unit = {}) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onVersionTap() }
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App logo circle (mirrors AboutScreen's AppHeroSection icon style)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PixChive",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Version $versionName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

//  Section label 

@Composable
private fun SettingsSectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

//  Rounded card wrapper 

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Column(content = content)
    }
}

//  Thin divider inside a card 

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp, end = 0.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

//  Standard clickable row 

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Leading icon in a small tinted circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

//  Toggle row 

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}