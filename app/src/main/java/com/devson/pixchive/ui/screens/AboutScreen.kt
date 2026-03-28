package com.devson.pixchive.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Data models

data class OpenSourceLibrary(
    val name: String,
    val author: String,
    val description: String,
    val url: String,
    val license: String,
    val category: String
)

// Library catalogue
private val openSourceLibraries = listOf(
    OpenSourceLibrary(
        name = "Telephoto",
        author = "Saket Narayan",
        description = "A zoomable image composable for Jetpack Compose, built on top of SubcomposeLayout. Powers the smooth pinch-to-zoom image viewing experience in PixChive.",
        url = "https://github.com/saket/telephoto",
        license = "Apache-2.0",
        category = "Image Handling"
    ),
    OpenSourceLibrary(
        name = "Coil",
        author = "Colin White & contributors",
        description = "An image loading library for Android backed by Kotlin Coroutines. Handles all thumbnail generation and image decoding efficiently in PixChive.",
        url = "https://github.com/coil-kt/coil",
        license = "Apache-2.0",
        category = "Image Loading"
    ),
    OpenSourceLibrary(
        name = "Jetpack Compose",
        author = "Google / Android Open Source Project",
        description = "Android's modern declarative UI toolkit. The entire PixChive UI is built with Compose - from the folder grid to the reader and settings screens.",
        url = "https://developer.android.com/jetpack/compose",
        license = "Apache-2.0",
        category = "UI Framework"
    ),
    OpenSourceLibrary(
        name = "Room",
        author = "Google / AOSP",
        description = "A persistence library that provides an abstraction layer over SQLite. Stores folder metadata, scan results, and user favourites in PixChive.",
        url = "https://developer.android.com/training/data-storage/room",
        license = "Apache-2.0",
        category = "Database"
    ),
    OpenSourceLibrary(
        name = "Navigation Compose",
        author = "Google / AOSP",
        description = "The official navigation library for Jetpack Compose. Powers screen-to-screen transitions and the back-stack throughout the app.",
        url = "https://developer.android.com/guide/navigation/navigation-compose",
        license = "Apache-2.0",
        category = "Navigation"
    ),
    OpenSourceLibrary(
        name = "DataStore",
        author = "Google / AOSP",
        description = "A data storage solution using Kotlin Coroutines and Flow. Persists all user preferences (theme, hidden-files toggle, etc.) in PixChive.",
        url = "https://developer.android.com/topic/libraries/architecture/datastore",
        license = "Apache-2.0",
        category = "Preferences"
    ),
    OpenSourceLibrary(
        name = "Paging 3",
        author = "Google / AOSP",
        description = "Load and display pages of data gracefully. Used by PixChive to stream large flat-view image lists without overwhelming memory.",
        url = "https://developer.android.com/topic/libraries/architecture/paging/v3-overview",
        license = "Apache-2.0",
        category = "Data Loading"
    ),
    OpenSourceLibrary(
        name = "WorkManager",
        author = "Google / AOSP",
        description = "A background task scheduler that respects battery and OS constraints. Powers deferred folder-scanning jobs in PixChive.",
        url = "https://developer.android.com/topic/libraries/architecture/workmanager",
        license = "Apache-2.0",
        category = "Background Work"
    ),
    OpenSourceLibrary(
        name = "Accompanist Permissions",
        author = "Google / Android Open Source Project",
        description = "A utility library for handling runtime permissions in Jetpack Compose. Used to request storage / media access permissions on first launch.",
        url = "https://google.github.io/accompanist/permissions/",
        license = "Apache-2.0",
        category = "Permissions"
    ),
    OpenSourceLibrary(
        name = "simple-storage",
        author = "Anggrayudi Hardiannico",
        description = "A wrapper around Android's Storage Access Framework that simplifies SAF URIs, DocumentFile operations, and cross-partition file I/O - critical for broad file support in PixChive.",
        url = "https://github.com/anggrayudi/SimpleStorage",
        license = "Apache-2.0",
        category = "File I/O"
    ),
    OpenSourceLibrary(
        name = "ExifInterface",
        author = "Google / AOSP",
        description = "Reads and writes EXIF metadata from JPEG/WebP/PNG files. Provides image orientation and metadata info in PixChive's detail view.",
        url = "https://developer.android.com/reference/androidx/exifinterface/media/ExifInterface",
        license = "Apache-2.0",
        category = "Metadata"
    ),
    OpenSourceLibrary(
        name = "Gson",
        author = "Google",
        description = "A Java serialization/deserialization library. Used internally for JSON-based folder data persistence in PixChive.",
        url = "https://github.com/google/gson",
        license = "Apache-2.0",
        category = "Serialization"
    ),
    OpenSourceLibrary(
        name = "Kotlin Coroutines",
        author = "JetBrains",
        description = "Asynchronous programming made simple. Every background operation in PixChive - scanning, loading, sorting - is powered by Kotlin coroutines and Flow.",
        url = "https://github.com/Kotlin/kotlinx.coroutines",
        license = "Apache-2.0",
        category = "Async"
    )
)

// Category chip colors mapped to MaterialTheme tokens (index-based for variety)
private val categoryColors = listOf(
    Color(0xFF6750A4), // purple
    Color(0xFF0B6E4F), // green
    Color(0xFFB5460B), // orange
    Color(0xFF006782), // teal
    Color(0xFF6B3FA0), // violet
    Color(0xFF7A5900), // amber
    Color(0xFF004BA1), // blue
)

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun categoryColor(category: String): Color {
    return categoryColors[Math.abs(category.hashCode()) % categoryColors.size]
}

// ---------------------------------------------------------------------------
// Composables
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ----------------------------------------------------------------
            // Developer / Links section
            // ----------------------------------------------------------------
            item {
                DeveloperSection(
                    onLinkClick = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                )
            }

            // ----------------------------------------------------------------
            // Open-source libraries header
            // ----------------------------------------------------------------
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Open Source Libraries",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "PixChive is built on the shoulders of these amazing open-source projects. Huge thanks to all the authors! 🙌",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                    HorizontalDivider()
                }
            }

            // ----------------------------------------------------------------
            // Library cards
            // ----------------------------------------------------------------
            items(openSourceLibraries) { library ->
                LibraryCard(
                    library = library,
                    onViewSource = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(library.url)))
                    }
                )
            }

            // ----------------------------------------------------------------
            // Footer
            // ----------------------------------------------------------------
            item {
                FooterSection()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Developer section
// ---------------------------------------------------------------------------

@Composable
private fun DeveloperSection(onLinkClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Developer",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "D",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "Devson",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Developer & designer of PixChive",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))

                LinkRow(
                    icon = Icons.Default.Code,
                    label = "GitHub Profile",
                    url = "https://github.com/DevSon1024",
                    onClick = onLinkClick
                )
                Spacer(Modifier.height(8.dp))
                LinkRow(
                    icon = Icons.Default.FolderOpen,
                    label = "PixChive Repository",
                    url = "https://github.com/DevSon1024/PixChive",
                    onClick = onLinkClick
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
    }
}

@Composable
private fun LinkRow(
    icon: ImageVector,
    label: String,
    url: String,
    onClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick(url) }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = url.removePrefix("https://"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Open link",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// Library card
// ---------------------------------------------------------------------------

@Composable
private fun LibraryCard(
    library: OpenSourceLibrary,
    onViewSource: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val catColor = categoryColor(library.category)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Color accent dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(catColor)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = library.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "by ${library.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Category chip
                Surface(
                    shape = RoundedCornerShape(50),
                    color = catColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp, catColor.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = library.category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = catColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Expand chevron
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expandable content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = library.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // License badge
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Gavel,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = library.license,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }

                        // View source button
                        OutlinedButton(
                            onClick = onViewSource,
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "View Source",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Footer
// ---------------------------------------------------------------------------

@Composable
private fun FooterSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Made with ❤️ by Devson",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Licensed under GNU GPL v3.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------
// Brush extension helper (linear gradient for Box)
// ---------------------------------------------------------------------------

private fun Brush.Companion.linearGradientBrush(colors: List<Color>) =
    linearGradient(colors)