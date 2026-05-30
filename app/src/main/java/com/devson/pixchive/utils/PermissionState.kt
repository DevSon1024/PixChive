package com.devson.pixchive.utils

sealed class PermissionState {
    object Granted : PermissionState()
    object Denied : PermissionState()
    object PermanentlyDenied : PermissionState()
    object NotRequested : PermissionState()
}

data class PermissionUiState(
    val permissionState: PermissionState = PermissionState.NotRequested,
    val showRationaleDialog: Boolean = false,
    val showSettingsDialog: Boolean = false
)
