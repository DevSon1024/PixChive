package com.devson.pixchive.ui.reader.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.pixchive.data.ImageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val clipboardManager = LocalClipboardManager.current
    var metadata by remember { mutableStateOf<ImageMetadata?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(image.uri) {
        metadata = loadImageMetadata(context, image)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.8f)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Image Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider()

                // Content
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    metadata?.let { data ->
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Basic Info Section
                            item {
                                SectionHeader("Basic Information")
                            }

                            item {
                                DetailRow("Name", data.name)
                                DetailRow("Size", data.size)
                                DetailRow("Dimensions", data.dimensions)
                                DetailRow("Type", data.mimeType ?: "Unknown")
                                DetailRow("Orientation", data.orientation ?: "Normal")
                            }

                            // Date Information
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                SectionHeader("Date Information")
                            }

                            item {
                                data.dateTaken?.let { DetailRow("Date Taken", it) }
                                data.dateModified?.let { DetailRow("Date Modified", it) }
                            }

                            // Location Section
                            if (data.gpsLocation != null) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SectionHeader("Location")
                                }
                                item {
                                    DetailRow("GPS Coordinates", data.gpsLocation)
                                }
                            }

                            // Camera Information
                            data.camera?.let { camera ->
                                if (camera.make != null || camera.model != null) {
                                    item {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        SectionHeader("Camera Information")
                                    }

                                    item {
                                        camera.make?.let { DetailRow("Camera Make", it) }
                                        camera.model?.let { DetailRow("Camera Model", it) }
                                        camera.iso?.let { DetailRow("ISO", it) }
                                        camera.focalLength?.let { DetailRow("Focal Length", it) }
                                        camera.aperture?.let { DetailRow("Aperture", "f/$it") }
                                        camera.exposureTime?.let { DetailRow("Exposure Time", "${it}s") }
                                        camera.flash?.let { DetailRow("Flash", it) }
                                        camera.whiteBalance?.let { DetailRow("White Balance", it) }
                                    }
                                }
                            }

                            // Software/Editor
                            if (data.software != null) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SectionHeader("Software")
                                }
                                item {
                                    DetailRow("Edited With", data.software)
                                }
                            }

                            // File Path (HUMAN READABLE)
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                SectionHeader("File Location")
                            }
                            item {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Path",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = data.readablePath,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(data.readablePath))
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = "Copy path",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
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

        // Convert URI to readable path
        val readablePath = convertUriToReadablePath(image.uri.toString())

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
                val latLong = FloatArray(2)
                if (exif.getLatLong(latLong)) {
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
            readablePath = convertUriToReadablePath(image.uri.toString()),
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