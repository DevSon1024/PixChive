package com.devson.pixchive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.pixchive.R
import com.devson.pixchive.model.Image
import com.devson.pixchive.utils.DetailedImageMetadata
import com.devson.pixchive.utils.formatDate
import com.devson.pixchive.utils.formatSize
import com.devson.pixchive.utils.getImageMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformationBottomSheet(
    selectedImages: Set<Image>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var metadataList by remember { mutableStateOf<List<DetailedImageMetadata>>(emptyList()) }
    var isLoading by remember { mutableStateOf(selectedImages.size == 1) }

    LaunchedEffect(selectedImages) {
        if (selectedImages.size == 1) {
            isLoading = true
            val meta = getImageMetadata(context, selectedImages.first())
            metadataList = listOf(meta)
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.information),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            if (selectedImages.size > 1) {
                // Multi-selection Info
                MultiSelectionInfo(selectedImages)
            } else if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (metadataList.isNotEmpty()) {
                // Single Image Info
                SingleImageInfo(metadataList.first())
            }
        }
    }
}

@Composable
private fun MultiSelectionInfo(videos: Set<Image>) {
    val totalSize = videos.sumOf { it.size }
    
    InfoSection(title = stringResource(R.string.info_general)) {
        InfoRow(label = stringResource(R.string.info_selected), value = stringResource(R.string.folder_images_count, videos.size))
        InfoRow(label = stringResource(R.string.folder_info_total_size), value = formatSize(totalSize))
    }
}

@Composable
private fun SingleImageInfo(metadata: DetailedImageMetadata) {
    val image = metadata.image

    // Section 1: About File
    InfoSection(title = stringResource(R.string.info_about_file)) {
        InfoRow(label = stringResource(R.string.info_file_name), value = image.title)
        InfoRow(label = stringResource(R.string.folder_info_location), value = image.uri)
        InfoRow(label = stringResource(R.string.info_file_size), value = formatSize(image.size))
        InfoRow(label = stringResource(R.string.info_file_date), value = formatDate(image.dateAdded))
    }
}

@Composable
private fun localizeMetadataKey(key: String): String {
    return when (key) {
        "Title" -> stringResource(R.string.info_label_title)
        "Resolution" -> stringResource(R.string.info_label_resolution)
        "Type" -> stringResource(R.string.info_label_type)
        "Source" -> stringResource(R.string.info_label_source)
        "External" -> stringResource(R.string.info_label_external)
        else -> key
    }
}

@Composable
private fun localizeMetadataValue(key: String, value: String): String {
    return when {
        key == "Type" && value == "Forced" -> stringResource(R.string.info_value_forced)
        value == "External" -> stringResource(R.string.info_label_external)
        value == "External File" -> stringResource(R.string.info_value_external_file)
        else -> value
    }
}

@Composable
private fun InfoSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f)
        )
    }
}
