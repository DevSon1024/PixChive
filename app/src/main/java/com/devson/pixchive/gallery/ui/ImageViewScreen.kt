package com.devson.pixchive.gallery.ui

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.devson.pixchive.gallery.data.models.GalleryImage
import com.devson.pixchive.gallery.viewmodel.GalleryFolderViewModel
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.rememberCoilZoomState
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.ui.draw.BlurredEdgeTreatment

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ImageViewScreen(
    bucketId: String,
    initialIndex: Int,
    onNavigateBack: () -> Unit,
    viewModel: GalleryFolderViewModel = viewModel()
) {
    val images by viewModel.images.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var controlsVisible by remember { mutableStateOf(true) }
    var showInfoDialog by remember { mutableStateOf<GalleryImage?>(null) }
    var showDeleteSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val view = LocalView.current
    val window = (context as Activity).window
    val insetsController = remember { WindowCompat.getInsetsController(window, view) }

    LaunchedEffect(Unit) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        } else {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            WindowCompat.setDecorFitsSystemWindows(window, true)
        }
    }

    LaunchedEffect(bucketId) {
        if (images.isEmpty()) viewModel.loadImages(bucketId)
    }

    if (isLoading || images.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, images.lastIndex),
        pageCount = { images.size }
    )

    val currentImage = images[pagerState.currentPage]
    val isBackgroundBlurEnabled by viewModel.isBackgroundBlurEnabled.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val isFavorite = favorites.contains(currentImage.uri.toString())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { controlsVisible = !controlsVisible }
    ) {
        if (isBackgroundBlurEnabled) {
            AsyncImage(
                model = currentImage.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 32.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            )
            // Dim layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { images[it].id }
        ) { page ->
            val zoomState = rememberCoilZoomState()
            CoilZoomAsyncImage(
                model = images[page].uri,
                contentDescription = null,
                zoomState = zoomState,
                modifier = Modifier.fillMaxSize(),
                onTap = { controlsVisible = !controlsVisible }
            )
        }

        // Top Bar
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val dateString = remember(currentImage.dateModified) {
                SimpleDateFormat("MMMM dd", Locale.getDefault())
                    .format(Date(currentImage.dateModified * 1000L))
            }
            val timeString = remember(currentImage.dateModified) {
                SimpleDateFormat("h:mm a", Locale.getDefault())
                    .format(Date(currentImage.dateModified * 1000L))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dateString,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = timeString,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                IconButton(
                    onClick = { showInfoDialog = currentImage },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Bar
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color.Transparent,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFF1C1C1E).copy(alpha = 0.85f),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActionIconButton(
                            icon = Icons.Default.Share,
                            contentDescription = "Share",
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_STREAM, currentImage.uri)
                                    type = "image/*"
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            }
                        )
                        ActionIconButton(
                            icon = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newUri(context.contentResolver, "Image", currentImage.uri)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Image copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                        ActionIconButton(
                            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color.Red else Color.White,
                            onClick = { viewModel.toggleFavorite(currentImage.uri) }
                        )
                        ActionIconButton(
                            icon = Icons.Default.Edit,
                            contentDescription = "Edit",
                            onClick = {
                                val editIntent = Intent(Intent.ACTION_EDIT).apply {
                                    setDataAndType(currentImage.uri, "image/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try {
                                    context.startActivity(Intent.createChooser(editIntent, "Edit with"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No editor found", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        ActionIconButton(
                            icon = Icons.Default.Delete,
                            contentDescription = "Delete",
                            onClick = { showDeleteSheet = true }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDeleteSheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Move to Trash?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "This item will be moved to your device trash.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.size(160.dp),
                    shadowElevation = 4.dp
                ) {
                    AsyncImage(
                        model = currentImage.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteSheet = false },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            viewModel.deleteImage(currentImage) {
                                showDeleteSheet = false
                                if (images.size <= 1) {
                                    onNavigateBack()
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }
    }

    if (showInfoDialog != null) {
        ModalBottomSheet(
            onDismissRequest = { showInfoDialog = null },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            ImageInfoContent(image = currentImage)
        }
    }
}

@Composable
private fun ActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = Color.White
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ImageInfoContent(image: GalleryImage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Information",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        InfoRow(label = "Filename", value = image.realPath.substringAfterLast('/'))
        InfoRow(label = "Path", value = image.realPath)
        InfoRow(label = "Resolution", value = "${image.width} x ${image.height}")
        InfoRow(label = "Size", value = formatFileSize(image.size))
        InfoRow(
            label = "Date Modified",
            value = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date(image.dateModified * 1000L))
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}