package com.devson.pixchive.feature.settings.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.devson.pixchive.core.designsystem.component.AnimatedPixchiveLogo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Data Models

data class OpenSourceLibrary(
    val name: String,
    val author: String,
    val description: String,
    val url: String,
    val license: String,
    val category: String
)

private val openSourceLibraries = listOf(
    OpenSourceLibrary(
        name = "Telephoto",
        author = "Saket Narayan",
        description = "A zoomable image composable for Jetpack Compose, built on top of SubcomposeLayout. Powers smooth pinch-to-zoom image viewing in PixChive.",
        url = "https://github.com/saket/telephoto",
        license = "Apache-2.0",
        category = "Image Handling"
    ),
    OpenSourceLibrary(
        name = "Coil",
        author = "Colin White & contributors",
        description = "An image loading library for Android backed by Kotlin Coroutines. Handles thumbnail caching and fast bitmap decoding.",
        url = "https://github.com/coil-kt/coil",
        license = "Apache-2.0",
        category = "Image Loading"
    ),
    OpenSourceLibrary(
        name = "Jetpack Compose",
        author = "Google / AOSP",
        description = "Android's modern declarative UI toolkit. The entire PixChive UI is built with Compose using Material Design 3 guidelines.",
        url = "https://developer.android.com/jetpack/compose",
        license = "Apache-2.0",
        category = "UI Framework"
    ),
    OpenSourceLibrary(
        name = "Room",
        author = "Google / AOSP",
        description = "A persistence library that provides an abstraction layer over SQLite. Stores folder metadata, scan results, and user favourites.",
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
        description = "A modern data storage solution using Coroutines and Flow. Persists all user preferences (theme, layout mode, grid columns).",
        url = "https://developer.android.com/topic/libraries/architecture/datastore",
        license = "Apache-2.0",
        category = "Preferences"
    ),
    OpenSourceLibrary(
        name = "Paging 3",
        author = "Google / AOSP",
        description = "Load and display pages of data gracefully. Used by PixChive to stream massive photo collections with near-zero UI jank.",
        url = "https://developer.android.com/topic/libraries/architecture/paging/v3-overview",
        license = "Apache-2.0",
        category = "Data Loading"
    ),
    OpenSourceLibrary(
        name = "WorkManager",
        author = "Google / AOSP",
        description = "Background task scheduler respecting battery and OS constraints. Powers deferred background media sync in PixChive.",
        url = "https://developer.android.com/topic/libraries/architecture/workmanager",
        license = "Apache-2.0",
        category = "Background Work"
    ),
    OpenSourceLibrary(
        name = "Accompanist Permissions",
        author = "Google / AOSP",
        description = "Utility library for handling runtime permissions in Jetpack Compose for seamless storage and media access.",
        url = "https://google.github.io/accompanist/permissions/",
        license = "Apache-2.0",
        category = "Permissions"
    ),
    OpenSourceLibrary(
        name = "simple-storage",
        author = "Anggrayudi Hardiannico",
        description = "SAF wrapper that simplifies Storage Access Framework operations and cross-partition file I/O for broad folder support.",
        url = "https://github.com/anggrayudi/SimpleStorage",
        license = "Apache-2.0",
        category = "File I/O"
    ),
    OpenSourceLibrary(
        name = "ExifInterface",
        author = "Google / AOSP",
        description = "Reads and writes EXIF metadata from JPEG, WebP, and PNG files. Powers image detail info and orientation parsing.",
        url = "https://developer.android.com/reference/androidx/exifinterface/media/ExifInterface",
        license = "Apache-2.0",
        category = "Metadata"
    ),
    OpenSourceLibrary(
        name = "Gson",
        author = "Google",
        description = "A Java serialization/deserialization library. Used for robust JSON data persistence.",
        url = "https://github.com/google/gson",
        license = "Apache-2.0",
        category = "Serialization"
    ),
    OpenSourceLibrary(
        name = "Kotlin Coroutines",
        author = "JetBrains",
        description = "Asynchronous programming and reactive Flow pipelines powering all background operations in PixChive.",
        url = "https://github.com/Kotlin/kotlinx.coroutines",
        license = "Apache-2.0",
        category = "Async"
    )
)


// Main About Screen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    var showCredits by remember { mutableStateOf(false) }

    if (showCredits) {
        CreditsScreen(onNavigateBack = { showCredits = false })
    } else {
        MainAboutContent(
            onNavigateBack = onNavigateBack,
            onShowCredits = { showCredits = true }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAboutContent(
    onNavigateBack: () -> Unit,
    onShowCredits: () -> Unit
) {
    val context = LocalContext.current

    val packageInfo = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) {
            null
        }
    }
    val versionName = packageInfo?.versionName ?: "1.0.0"
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode?.toString() ?: "1"
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode?.toString() ?: "1"
    }

    val androidVersion = Build.VERSION.RELEASE
    val apiLevel = Build.VERSION.SDK_INT
    val abis = Build.SUPPORTED_ABIS.joinToString(", ")

    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About PixChive", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "PixChive")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out PixChive — a modern, fast, and private offline gallery & comic reader for Android!\nhttps://github.com/DevSon1024/PixChive"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share PixChive"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share App")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 40.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero App Identity Card
            item {
                AppHeroCard(
                    versionName = versionName,
                    versionCode = versionCode
                )
            }

            // 2. Build & System Info Card
            item {
                SystemInfoCard(
                    versionName = versionName,
                    versionCode = versionCode,
                    androidVersion = androidVersion,
                    apiLevel = apiLevel,
                    abis = abis,
                    onCopy = {
                        val buildInfo = """
                            PixChive: v$versionName ($versionCode)
                            Android OS: $androidVersion (API $apiLevel)
                            Device: ${Build.MANUFACTURER} ${Build.MODEL}
                            ABIs: $abis
                        """.trimIndent()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("PixChive System Info", buildInfo)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "System details copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 3. Project & Community Section
            item {
                AboutSectionLabel("Project & Community")
                M3CardContainer {
                    M3ActionRow(
                        icon = Icons.Outlined.Code,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Source Code",
                        subtitle = "Explore the repository on GitHub",
                        onClick = { openUrl("https://github.com/DevSon1024/PixChive") }
                    )
                    M3RowDivider()
                    M3ActionRow(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = "Readme & Documentation",
                        subtitle = "Learn about features and architecture",
                        onClick = { openUrl("https://github.com/DevSon1024/PixChive/blob/main/README.md") }
                    )
                    M3RowDivider()
                    M3ActionRow(
                        icon = Icons.Outlined.NewReleases,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        title = "Releases & Changelog",
                        subtitle = "View latest updates and release notes",
                        onClick = { openUrl("https://github.com/DevSon1024/PixChive/releases") }
                    )
                    M3RowDivider()
                    M3ActionRow(
                        icon = Icons.Outlined.BugReport,
                        iconTint = MaterialTheme.colorScheme.error,
                        title = "Report an Issue",
                        subtitle = "Submit bugs or request new features",
                        onClick = { openUrl("https://github.com/DevSon1024/PixChive/issues/new") }
                    )
                    M3RowDivider()
                    M3ActionRow(
                        icon = Icons.AutoMirrored.Filled.Send,
                        iconTint = Color(0xFF2AABEE), // Telegram blue accent
                        title = "Telegram Channel",
                        subtitle = "Join our community updates @pixchive",
                        onClick = { openUrl("https://t.me/pixchive") }
                    )
                }
            }

            // 4. Developer & Contribution Section
            item {
                AboutSectionLabel("Developer")
                DeveloperCard(
                    onGitHubClick = { openUrl("https://github.com/DevSon1024") },
                    onSponsorClick = { openUrl("https://github.com/sponsors/DevSon1024") }
                )
            }

            // 5. Open Source & Licenses Section
            item {
                AboutSectionLabel("Acknowledgements")
                M3CardContainer {
                    M3ActionRow(
                        icon = Icons.Outlined.VolunteerActivism,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Open Source Libraries",
                        subtitle = "${openSourceLibraries.size} libraries powering PixChive",
                        trailing = {
                            FilledTonalButton(
                                onClick = onShowCredits,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text("View All", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        },
                        onClick = onShowCredits
                    )
                }
            }

            // 6. Footer Section
            item {
                AboutFooter()
            }
        }
    }
}


// Hero App Card (Material You)


@Composable
private fun AppHeroCard(
    versionName: String,
    versionCode: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated PixChive Logo
            AnimatedPixchiveLogo(
                modifier = Modifier.size(104.dp),
                color = MaterialTheme.colorScheme.primary,
                maskColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )

            Spacer(Modifier.height(16.dp))

            // App Name
            Text(
                text = "PixChive",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(4.dp))

            // Tagline
            Text(
                text = "Modern, Fast & Private Gallery & Reader",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))

            // Feature Badges
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroBadge(text = "v$versionName ($versionCode)", icon = Icons.Outlined.CheckCircle)
                HeroBadge(text = "Material You", icon = Icons.Outlined.Palette)
                HeroBadge(text = "100% Offline", icon = Icons.Outlined.Shield)
                HeroBadge(text = "Open Source", icon = Icons.Outlined.Code)
            }
        }
    }
}

@Composable
private fun HeroBadge(text: String, icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


// System Info Card


@Composable
private fun SystemInfoCard(
    versionName: String,
    versionCode: String,
    androidVersion: String,
    apiLevel: Int,
    abis: String,
    onCopy: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .animateContentSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Version $versionName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Build $versionCode · Android $androidVersion (API $apiLevel)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onCopy) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy Build Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))

                    InfoRowItem(label = "Device", value = "${Build.MANUFACTURER} ${Build.MODEL}")
                    InfoRowItem(label = "Android Release", value = "Android $androidVersion (API $apiLevel)")
                    InfoRowItem(label = "Architecture", value = abis)
                    InfoRowItem(label = "Package", value = "com.devson.pixchive")
                }
            }
        }
    }
}

@Composable
private fun InfoRowItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


// Developer Card


@Composable
private fun DeveloperCard(
    onGitHubClick: () -> Unit,
    onSponsorClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Developer Initial Badge
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "D",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Devson",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Author",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Creator & maintainer of PixChive",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onGitHubClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Code,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("GitHub Profile", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Button(
                    onClick = onSponsorClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Sponsor", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}


// Credits / Open Source Screen (Filtered & Material You)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreditsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val categories = remember {
        listOf("All") + openSourceLibraries.map { it.category }.distinct()
    }
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredLibraries = remember(selectedCategory) {
        if (selectedCategory == "All") {
            openSourceLibraries
        } else {
            openSourceLibraries.filter { it.category == selectedCategory }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open Source Credits", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 32.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Info Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Built with Open Source ❤️",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "PixChive stands on the shoulders of these remarkable open-source projects. Immense gratitude to all developers and maintainers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Category Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            shape = RoundedCornerShape(50)
                        )
                    }
                }
            }

            // Library Cards
            items(filteredLibraries, key = { it.name }) { library ->
                ModernLibraryCard(
                    library = library,
                    onViewSource = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(library.url)))
                        } catch (_: Exception) {
                            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ModernLibraryCard(
    library: OpenSourceLibrary,
    onViewSource: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category icon dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = library.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "by ${library.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Category pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = library.category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = library.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiaryContainer
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

                        OutlinedButton(
                            onClick = onViewSource,
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Source", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}


// Common Material You UI Components


@Composable
private fun AboutSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun M3CardContainer(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            content = content
        )
    }
}

@Composable
private fun M3ActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = iconTint
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun M3RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun AboutFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Crafted with ❤️ by Devson",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = "Licensed under GNU General Public License v3.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}