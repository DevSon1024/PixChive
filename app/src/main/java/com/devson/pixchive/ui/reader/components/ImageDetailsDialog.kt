package com.devson.pixchive.ui.reader.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import com.devson.pixchive.data.ImageFile
import com.devson.pixchive.data.local.ImageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

data class ImageMetadata(
    val name: String,
    val size: String,
    val dimensions: String,
    val path: String,
    val readablePath: String,
    val dateModified: String?,
    val dateTaken: String?,
    val mimeType: String?,
    val orientation: String?,
    val gpsLocation: String?,
    val camera: CameraMetadata?,
    val software: String?
)

data class CameraMetadata(
    val make: String?,
    val model: String?,
    val iso: String?,
    val focalLength: String?,
    val aperture: String?,
    val exposureTime: String?,
    val flash: String?,
    val whiteBalance: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailsDialog(
    image: ImageFile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var metadata by remember { mutableStateOf<ImageMetadata?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(image.uri) {
        metadata = loadImageMetadata(context, image)
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            metadata?.let { data ->
                ImageDetailsSheetContent(data = data, onDismiss = onDismiss)
            } ?: Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Failed to load metadata")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailsDialog(
    entity: ImageEntity,
    onDismiss: () -> Unit
) {
    var metadata by remember { mutableStateOf<ImageMetadata?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(entity.path) {
        metadata = loadImageMetadataFromFile(entity)
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            metadata?.let { data ->
                ImageDetailsSheetContent(data = data, onDismiss = onDismiss)
            } ?: Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Failed to load metadata")
            }
        }
    }
}

@Composable
private fun ImageDetailsSheetContent(
    data: ImageMetadata,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Image Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = data.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onDismiss,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Card 1: File Specs
            item {
                MetadataCategoryCard(
                    title = "File Specifications",
                    icon = Icons.Default.Description
                ) {
                    DetailRow(
                        icon = Icons.Default.Info,
                        label = "File Name",
                        value = data.name,
                        onCopyClick = {
                            scope.launch {
                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("File Name", data.name)))
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    DetailRow(
                        icon = Icons.Default.Storage,
                        label = "Size",
                        value = data.size
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    DetailRow(
                        icon = Icons.Default.AspectRatio,
                        label = "Dimensions",
                        value = data.dimensions
                    )
                    data.mimeType?.let {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        DetailRow(
                            icon = Icons.Default.Extension,
                            label = "Format",
                            value = it
                        )
                    }
                    data.orientation?.let {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        DetailRow(
                            icon = Icons.Default.ScreenRotation,
                            label = "Orientation",
                            value = it
                        )
                    }
                }
            }

            // Card 2: Dates
            if (data.dateTaken != null || data.dateModified != null) {
                item {
                    MetadataCategoryCard(
                        title = "Timestamps",
                        icon = Icons.Default.CalendarToday
                    ) {
                        data.dateTaken?.let {
                            DetailRow(
                                icon = Icons.Default.PhotoCamera,
                                label = "Date Taken",
                                value = it
                            )
                        }
                        if (data.dateTaken != null && data.dateModified != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                        data.dateModified?.let {
                            DetailRow(
                                icon = Icons.Default.EditCalendar,
                                label = "Date Modified",
                                value = it
                            )
                        }
                    }
                }
            }

            // Card 3: Camera details (EXIF)
            data.camera?.let { camera ->
                if (camera.make != null || camera.model != null || camera.iso != null) {
                    item {
                        MetadataCategoryCard(
                            title = "Camera Specifications",
                            icon = Icons.Default.PhotoCamera
                        ) {
                            var needsDivider = false

                            camera.make?.let {
                                DetailRow(
                                    icon = Icons.Default.Settings,
                                    label = "Camera Manufacturer",
                                    value = it
                                )
                                needsDivider = true
                            }

                            camera.model?.let {
                                if (needsDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                DetailRow(
                                    icon = Icons.Default.CameraAlt,
                                    label = "Camera Model",
                                    value = it
                                )
                                needsDivider = true
                            }

                            camera.iso?.let {
                                if (needsDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                DetailRow(
                                    icon = Icons.Default.Speed,
                                    label = "ISO Sensitivity",
                                    value = it
                                )
                                needsDivider = true
                            }

                            camera.aperture?.let {
                                if (needsDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                DetailRow(
                                    icon = Icons.Default.Lens,
                                    label = "Aperture",
                                    value = "f/$it"
                                )
                                needsDivider = true
                            }

                            camera.focalLength?.let {
                                if (needsDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                DetailRow(
                                    icon = Icons.Default.FilterCenterFocus,
                                    label = "Focal Length",
                                    value = it
                                )
                                needsDivider = true
                            }

                            camera.exposureTime?.let {
                                if (needsDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                DetailRow(
                                    icon = Icons.Default.Timer,
                                    label = "Exposure Time",
                                    value = "${it}s"
                                )
                                needsDivider = true
                            }

                            camera.flash?.let {
                                if (needsDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                DetailRow(
                                    icon = Icons.Default.FlashOn,
                                    label = "Flash Mode",
                                    value = it
                                )
                                needsDivider = true
                            }

                            camera.whiteBalance?.let {
                                if (needsDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                DetailRow(
                                    icon = Icons.Default.WbSunny,
                                    label = "White Balance",
                                    value = it
                                )
                            }
                        }
                    }
                }
            }

            // Card 4: Location
            data.gpsLocation?.let { gps ->
                item {
                    MetadataCategoryCard(
                        title = "Location Data",
                        icon = Icons.Default.LocationOn
                    ) {
                        DetailRow(
                            icon = Icons.Default.Map,
                            label = "GPS Coordinates",
                            value = gps,
                            onCopyClick = {
                                scope.launch {
                                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Coordinates", gps)))
                                }
                            }
                        )
                    }
                }
            }

            // Card 5: Path & Software info
            item {
                MetadataCategoryCard(
                    title = "File Location & Software",
                    icon = Icons.Default.FolderOpen
                ) {
                    DetailRow(
                        icon = Icons.Default.Folder,
                        label = "Directory Path",
                        value = data.readablePath,
                        onCopyClick = {
                            scope.launch {
                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Path", data.readablePath)))
                            }
                        }
                    )
                    data.software?.let {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        DetailRow(
                            icon = Icons.Default.Computer,
                            label = "Edited With / Software",
                            value = it
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataCategoryCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    onCopyClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (onCopyClick != null) {
            IconButton(
                onClick = onCopyClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private suspend fun loadImageMetadata(
    context: Context,
    image: ImageFile
): ImageMetadata = withContext(Dispatchers.IO) {
    try {
        var width = 0
        var height = 0
        var dateModified: String? = null
        var dateTaken: String? = null
        var mimeType: String? = null
        var orientation: String? = null
        var gpsLocation: String? = null
        var camera: CameraMetadata? = null
        var software: String? = null

        // Try to get a valid filesystem path
        var finalPath = if (image.path.isNotBlank() && (image.path.startsWith("/") || image.path.contains(":")) && !image.path.startsWith("content://")) {
            image.path
        } else {
            ""
        }

        // If path is missing or restricted, try querying MediaStore columns via ContentResolver
        if (finalPath.isBlank() && image.uri.scheme == "content") {
            try {
                val projection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    arrayOf(
                        android.provider.MediaStore.Images.Media.DATA,
                        android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                        android.provider.MediaStore.Images.Media.DISPLAY_NAME
                    )
                } else {
                    arrayOf(android.provider.MediaStore.Images.Media.DATA)
                }

                context.contentResolver.query(image.uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        // Try DATA first
                        val dataIndex = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.DATA)
                        if (dataIndex != -1) {
                            finalPath = cursor.getString(dataIndex) ?: ""
                        }
                        
                        // Fallback to RELATIVE_PATH + DISPLAY_NAME on Android 10+
                        if (finalPath.isBlank() && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            val relIndex = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.RELATIVE_PATH)
                            val nameIndex = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.DISPLAY_NAME)
                            if (relIndex != -1 && nameIndex != -1) {
                                val relPath = cursor.getString(relIndex) ?: ""
                                val name = cursor.getString(nameIndex) ?: ""
                                if (relPath.isNotBlank() && name.isNotBlank()) {
                                    // Construct the path (assuming external storage)
                                    finalPath = "/storage/emulated/0/$relPath$name"
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val readablePath = if (finalPath.isNotBlank()) finalPath else convertUriToReadablePath(image.uri.toString())

        // Get basic image dimensions
        context.contentResolver.openInputStream(image.uri)?.use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            width = options.outWidth
            height = options.outHeight
            mimeType = options.outMimeType
        }

        // Get file metadata
        context.contentResolver.query(
            image.uri,
            arrayOf(
                android.provider.MediaStore.Images.Media.DATE_MODIFIED,
                android.provider.MediaStore.Images.Media.DATE_TAKEN,
                android.provider.MediaStore.Images.Media.MIME_TYPE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val dateModifiedIndex = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.DATE_MODIFIED)
                val dateTakenIndex = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.DATE_TAKEN)
                val mimeTypeIndex = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.MIME_TYPE)

                if (dateModifiedIndex >= 0) {
                    val timestamp = cursor.getLong(dateModifiedIndex)
                    dateModified = formatDate(timestamp * 1000)
                }

                if (dateTakenIndex >= 0) {
                    val timestamp = cursor.getLong(dateTakenIndex)
                    if (timestamp > 0) {
                        dateTaken = formatDate(timestamp)
                    }
                }

                if (mimeTypeIndex >= 0) {
                    mimeType = cursor.getString(mimeTypeIndex)
                }
            }
        }

        // Get EXIF data
        context.contentResolver.openInputStream(image.uri)?.use { inputStream ->
            try {
                val exif = ExifInterface(inputStream)

                // Orientation
                orientation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> "Rotated 90°"
                    ExifInterface.ORIENTATION_ROTATE_180 -> "Rotated 180°"
                    ExifInterface.ORIENTATION_ROTATE_270 -> "Rotated 270°"
                    else -> "Normal"
                }

                // GPS Location
                val latLong = exif.latLong
                if (latLong != null) {
                    gpsLocation = "${latLong[0]}, ${latLong[1]}"
                }

                // Camera Info
                val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                val focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
                val aperture = exif.getAttribute(ExifInterface.TAG_APERTURE_VALUE)
                val exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
                val flash = exif.getAttribute(ExifInterface.TAG_FLASH)
                val whiteBalance = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE)

                if (make != null || model != null) {
                    camera = CameraMetadata(
                        make = make,
                        model = model,
                        iso = iso,
                        focalLength = focalLength?.let { "${it}mm" },
                        aperture = aperture,
                        exposureTime = exposureTime,
                        flash = flash?.let { if (it == "1") "Fired" else "Not Fired" },
                        whiteBalance = whiteBalance?.let { if (it == "0") "Auto" else "Manual" }
                    )
                }

                // Software
                software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)

                // Date taken from EXIF (if not found earlier)
                if (dateTaken == null) {
                    exif.getAttribute(ExifInterface.TAG_DATETIME)?.let {
                        dateTaken = formatExifDate(it)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        ImageMetadata(
            name = image.name,
            size = formatFileSize(image.size),
            dimensions = "${width} × ${height}",
            path = image.path,
            readablePath = readablePath,
            dateModified = dateModified,
            dateTaken = dateTaken,
            mimeType = mimeType,
            orientation = orientation,
            gpsLocation = gpsLocation,
            camera = camera,
            software = software
        )
    } catch (e: Exception) {
        e.printStackTrace()
        ImageMetadata(
            name = image.name,
            size = formatFileSize(image.size),
            dimensions = "Unknown",
            path = image.path,
            readablePath = if (image.path.startsWith("/") || image.path.contains(":")) image.path else convertUriToReadablePath(image.uri.toString()),
            dateModified = null,
            dateTaken = null,
            mimeType = null,
            orientation = null,
            gpsLocation = null,
            camera = null,
            software = null
        )
    }
}

/**
 * Variant of [loadImageMetadata] that reads from an absolute file path.
 * Used for [ImageEntity] images whose URIs are file:// URIs generated by FolderScanner.
 */
private suspend fun loadImageMetadataFromFile(
    entity: ImageEntity
): ImageMetadata = withContext(Dispatchers.IO) {
    try {
        val file = File(entity.path)
        var width = 0
        var height = 0
        var mimeType: String? = null
        var orientation: String? = null
        var gpsLocation: String? = null
        var camera: CameraMetadata? = null
        var software: String? = null
        var dateTaken: String? = null
        val dateModified = if (entity.dateModified > 0) formatDate(entity.dateModified) else null

        // Dimensions + MIME type
        if (file.exists()) {
            FileInputStream(file).use { inputStream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(inputStream, null, options)
                width = options.outWidth
                height = options.outHeight
                mimeType = options.outMimeType
            }
        }

        // EXIF data via file path (works without contentResolver)
        if (file.exists()) {
            try {
                val exif = ExifInterface(entity.path)

                orientation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90  -> "Rotated 90°"
                    ExifInterface.ORIENTATION_ROTATE_180 -> "Rotated 180°"
                    ExifInterface.ORIENTATION_ROTATE_270 -> "Rotated 270°"
                    else -> "Normal"
                }

                exif.latLong?.let { gpsLocation = "${it[0]}, ${it[1]}" }

                val make  = exif.getAttribute(ExifInterface.TAG_MAKE)
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                if (make != null || model != null) {
                    camera = CameraMetadata(
                        make          = make,
                        model         = model,
                        iso           = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY),
                        focalLength   = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { "${it}mm" },
                        aperture      = exif.getAttribute(ExifInterface.TAG_APERTURE_VALUE),
                        exposureTime  = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME),
                        flash         = exif.getAttribute(ExifInterface.TAG_FLASH)?.let { if (it == "1") "Fired" else "Not Fired" },
                        whiteBalance  = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE)?.let { if (it == "0") "Auto" else "Manual" }
                    )
                }

                software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)

                exif.getAttribute(ExifInterface.TAG_DATETIME)?.let { dateTaken = formatExifDate(it) }
            } catch (e: Exception) { e.printStackTrace() }
        }

        ImageMetadata(
            name          = entity.name,
            size          = entity.formattedSize.ifEmpty { formatFileSize(entity.size) },
            dimensions    = "$width × $height",
            path          = entity.path,
            readablePath  = entity.path,
            dateModified  = dateModified,
            dateTaken     = dateTaken,
            mimeType      = mimeType,
            orientation   = orientation,
            gpsLocation   = gpsLocation,
            camera        = camera,
            software      = software
        )
    } catch (e: Exception) {
        e.printStackTrace()
        ImageMetadata(
            name          = entity.name,
            size          = entity.formattedSize.ifEmpty { formatFileSize(entity.size) },
            dimensions    = "Unknown",
            path          = entity.path,
            readablePath  = entity.path,
            dateModified  = null,
            dateTaken     = null,
            mimeType      = null,
            orientation   = null,
            gpsLocation   = null,
            camera        = null,
            software      = null
        )
    }
}

// Convert URI to human-readable path
private fun convertUriToReadablePath(uriString: String): String {
    return try {
        // Decode URL encoding
        var decoded = URLDecoder.decode(uriString, StandardCharsets.UTF_8.toString())

        // Extract the actual path from content:// URI
        when {
            decoded.contains("/document/") -> {
                // Extract document path
                val documentPath = decoded.substringAfter("/document/")

                // Replace %3A with : and %2F with /
                var readable = documentPath
                    .replace("%3A", ":")
                    .replace("%2F", "/")
                    .replace("%20", " ")

                // Remove "primary:" prefix and add /storage/emulated/0/
                if (readable.startsWith("primary:")) {
                    readable = "/storage/emulated/0/" + readable.removePrefix("primary:")
                }

                readable
            }
            decoded.contains("/tree/") -> {
                // Extract tree path
                val treePath = decoded.substringAfter("/tree/").substringBefore("/document")

                var readable = treePath
                    .replace("%3A", ":")
                    .replace("%2F", "/")
                    .replace("%20", " ")

                if (readable.startsWith("primary:")) {
                    readable = "/storage/emulated/0/" + readable.removePrefix("primary:")
                }

                readable
            }
            decoded.contains("content://media/") -> {
                // Try to extract the ID and at least show which media it is
                val id = decoded.substringAfterLast("/")
                val type = when {
                    decoded.contains("/images/") -> "Images"
                    decoded.contains("/video/") -> "Videos"
                    else -> "Media"
                }
                "Internal Storage → $type → $id"
            }
            else -> {
                // Fallback: just decode URL encoding
                decoded
                    .replace("%3A", ":")
                    .replace("%2F", "/")
                    .replace("%20", " ")
            }
        }
    } catch (e: Exception) {
        uriString
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
        else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatExifDate(exifDate: String): String {
    return try {
        val exifFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
        val date = exifFormat.parse(exifDate)
        val displayFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        date?.let { displayFormat.format(it) } ?: exifDate
    } catch (e: Exception) {
        exifDate
    }
}