package com.nihaltp.aftersleep.ui.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionStateSnapshotTest {
    @Test
    fun `allCorePermissionsGranted is true only when notification and listener are granted`() {
        assertFalse(PermissionStateSnapshot().allCorePermissionsGranted)
        assertFalse(
            PermissionStateSnapshot(
                notificationPermissionGranted = true,
                listenerAccessGranted = false,
            ).allCorePermissionsGranted,
        )
        assertFalse(
            PermissionStateSnapshot(
                notificationPermissionGranted = false,
                listenerAccessGranted = true,
            ).allCorePermissionsGranted,
        )
        assertTrue(
            PermissionStateSnapshot(
                notificationPermissionGranted = true,
                listenerAccessGranted = true,
            ).allCorePermissionsGranted,
        )
    }
}
