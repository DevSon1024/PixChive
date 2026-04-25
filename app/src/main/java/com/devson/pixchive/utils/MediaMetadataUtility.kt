package com.devson.pixchive.utils

import android.content.Context
import com.devson.pixchive.model.Image

data class DetailedImageMetadata(
    val image: Image,
    val resolution: String,
)

suspend fun getImageMetadata(
    context: Context,
    image: Image
): DetailedImageMetadata {
    return DetailedImageMetadata(image, "Unknown")
}