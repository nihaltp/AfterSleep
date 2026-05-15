package com.nihaltp.aftersleep.ui.model

data class PermissionStateSnapshot(
    val notificationPermissionGranted: Boolean = false,
    val listenerAccessGranted: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false,
) {
    val allCorePermissionsGranted: Boolean
        get() = notificationPermissionGranted && listenerAccessGranted
}
