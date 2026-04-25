package com.devson.pixchive.ui.screens.imagelist.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.devson.pixchive.model.Image
import java.util.ArrayList

fun shareImages(context: Context, images: List<Image>) {
    if (images.isEmpty()) return
    val uris = images.map { Uri.parse(it.uri) }
    
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
