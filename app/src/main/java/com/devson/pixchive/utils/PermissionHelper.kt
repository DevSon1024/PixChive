package com.devson.pixchive.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionHelper {

    /**
     * Get required storage permission based on Android version
     */
    fun getStoragePermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    /**
     * Check if storage permission is granted
     */
    fun hasStoragePermission(context: Context): Boolean {
        val permission = getStoragePermission()
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Open app settings page
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Get user-friendly permission explanation
     */
    fun getPermissionRationale(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            "PixChive needs access to your photos to display comic images from your folders."
        } else {
            "PixChive needs storage permission to read comic images from your device."
        }
    }

    /**
     * Check if we should show permission rationale
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        val permission = getStoragePermission()
        return activity.shouldShowRequestPermissionRationale(permission)
    }
}
