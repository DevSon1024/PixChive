package com.devson.pixchive.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import com.devson.pixchive.data.ImageFile
import com.devson.pixchive.data.PreferencesManager
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateBack: () -> Unit,
    onImageClick: (String, Int) -> Unit // Pass folderId, index
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val favorites by prefs.favoritesFlow.collectAsState(initial = emptySet())

    // Convert favorite URIs to ImageFiles
    val favImages = remember(favorites) {
        favorites.mapNotNull { uriString ->
            val uri = Uri.parse(uriString)
            // Best effort to get name/size. Performance note: accessing DocumentFile/File here might be slow on main thread
            // but acceptable for favorites list which is usually small.
            val name = DocumentFile.fromSingleUri(context, uri)?.name ?: "Unknown"
            ImageFile(name, uri.path ?: "", uri, 0, 0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Reusing FlatView logic but for favorites
            // Note: onImageClick might need adjustment to handle specific favorite navigation logic
            // or just open ReaderScreen with this specific list.
            if (favImages.isEmpty()) {
                Text("No favorites yet", Modifier.align(androidx.compose.ui.Alignment.Center))
            } else {
                FlatView(
                    images = favImages,
                    layoutMode = "grid", // Default to grid for favorites
                    gridColumns = 3,
                    onImageClick = { /* Handle click - likely needs a specific route or passing this list to reader */ },
                    onRefresh = {}
                )
            }
        }
    }
}