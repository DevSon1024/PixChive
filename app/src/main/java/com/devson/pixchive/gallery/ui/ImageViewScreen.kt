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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.devson.pixchive.gallery.data.models.GalleryImage
import com.devson.pixchive.gallery.viewmodel.GalleryFolderViewModel
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.devson.pixchive.ui.reader.components.ImageDetailsDialog
import com.devson.pixchive.data.ImageFile
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
import com.devson.pixchive.viewmodel.FileOperationsViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.ui.draw.BlurredEdgeTreatment

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ImageViewScreen(
    bucketId: String,
    initialIndex: Int,
    onNavigateBack: () -> Unit,
    viewModel: GalleryFolderViewModel = viewModel(),
    fileOpsViewModel: FileOperationsViewModel = viewModel()
) {
    val images by viewModel.images.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var controlsVisible by remember { mutableStateOf(true) }
    var showInfoDialog by remember { mutableStateOf<GalleryImage?>(null) }
    var showDeleteSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val pendingIntentSender by fileOpsViewModel.pendingIntentSender.collectAsState()

    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fileOpsViewModel.onPermissionGranted(context)
            onNavigateBack()
        }
    }

    LaunchedEffect(pendingIntentSender) {
        pendingIntentSender?.let { intentSender ->
            val request = IntentSenderRequest.Builder(intentSender).build()
            intentSenderLauncher.launch(request)
            fileOpsViewModel.clearPendingIntentSender()
        }
    }

    val view = LocalView.current
    val window = (context as Activity).window
    val insetsController = remember { WindowCompat.getInsetsController(window, view) }

    DisposableEffect(window, view) {
        val originalLightStatus = insetsController.isAppearanceLightStatusBars
        val originalLightNav = insetsController.isAppearanceLightNavigationBars
        val originalBehavior = insetsController.systemBarsBehavior

        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        onDispose {
            insetsController.isAppearanceLightStatusBars = originalLightStatus
            insetsController.isAppearanceLightNavigationBars = originalLightNav
            insetsController.systemBarsBehavior = originalBehavior
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
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
            Crossfade(
                targetState = currentImage,
                animationSpec = tween(durationMillis = 500),
                label = "backgroundBlurCrossfade"
            ) { image ->
                AsyncImage(
                    model = image.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = 32.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                )
            }
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
                scrollBar = null,
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
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Delete photo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )

                    IconButton(
                        onClick = { showDeleteSheet = false },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "Choose what to do with this photo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 2.dp,
                    tonalElevation = 1.dp,
                    modifier = Modifier.size(96.dp)
                ) {
                    AsyncImage(
                        model = currentImage.uri,
                        contentDescription = "Image preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DeleteActionItem(
                        icon = Icons.Default.DeleteOutline,
                        title = "Move to Recycle Bin",
                        subtitle = "Restore later if needed",
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = {
                            fileOpsViewModel.deleteImages(
                                context,
                                listOf(currentImage.uri),
                                trash = true
                            )
                            showDeleteSheet = false
                            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
                                onNavigateBack()
                            }
                        }
                    )

                    DeleteActionItem(
                        icon = Icons.Default.Delete,
                        title = "Delete Permanently",
                        subtitle = "This cannot be undone",
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = {
                            fileOpsViewModel.deleteImages(
                                context,
                                listOf(currentImage.uri),
                                trash = false
                            )
                            showDeleteSheet = false
                            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
                                onNavigateBack()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }

    if (showInfoDialog != null) {
        ImageDetailsDialog(
            image = ImageFile(
                name = currentImage.realPath.substringAfterLast('/'),
                path = currentImage.realPath,
                uri = currentImage.uri,
                size = currentImage.size,
                dateModified = currentImage.dateModified
            ),
            onDismiss = { showInfoDialog = null }
        )
    }
}

@Composable
private fun DeleteActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
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
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
