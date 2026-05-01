package com.devson.pixchive.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.devson.pixchive.gallery.data.models.GalleryImage
import java.util.ArrayList

fun shareMedia(context: Context, images: List<GalleryImage>) {
    if (images.isEmpty()) return
    val uris = images.map { it.uri }
    
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uris.first())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    context.startActivity(Intent.createChooser(intent, "Share Image"))
}
