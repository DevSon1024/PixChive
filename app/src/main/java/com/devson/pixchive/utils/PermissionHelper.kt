package com.devson.pixchive.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionHelper {

    /**
     * Check if the app has the necessary storage permissions.
     * On Android 11+ (SDK 30+), this checks for MANAGE_EXTERNAL_STORAGE.
     * On older versions, it checks for READ_EXTERNAL_STORAGE.
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Get the Intent to request the appropriate permission.
     * On Android 11+, opens the "All Files Access" settings page.
     * On older versions, opens the App Details settings page.
     */
    fun getStoragePermissionSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    /**
     * Get the permission string for the standard permission launcher (Android 10 and below).
     * Note: This is NOT used for Android 11+ MANAGE_EXTERNAL_STORAGE request.
     */
    fun getLegacyStoragePermission(): String {
        return Manifest.permission.READ_EXTERNAL_STORAGE
    }

    /**
     * Get user-friendly permission explanation
     */
    fun getPermissionRationale(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "PixChive needs 'All Files Access' to quickly scan your folders and display hidden files (starting with .). Please grant this permission in the next screen."
        } else {
            "PixChive needs storage permission to read comic images from your device."
        }
    }

    /**
     * Check if we should show permission rationale (Legacy only)
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return false
        return activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}