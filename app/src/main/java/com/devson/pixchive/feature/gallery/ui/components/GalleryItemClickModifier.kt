package com.devson.pixchive.feature.gallery.ui.components

import androidx.compose.foundation.Indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import com.devson.pixchive.core.designsystem.component.galleryItemClick as coreGalleryItemClick

/**
 * Delegated to core design system click modifier.
 */
fun Modifier.galleryItemClick(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null
): Modifier = this.coreGalleryItemClick(
    onClick = onClick,
    onLongClick = onLongClick,
    interactionSource = interactionSource,
    indication = indication
)