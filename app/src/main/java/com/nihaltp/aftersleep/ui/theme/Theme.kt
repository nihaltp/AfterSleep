package com.nihaltp.aftersleep.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private fun baseColors(monochrome: Boolean): ColorScheme {
    val primary = if (monochrome) SleepOnSurface else SleepPrimary
    val secondary = if (monochrome) SleepOnSurfaceMuted else SleepPrimaryMuted
    return darkColorScheme(
        primary = primary,
        onPrimary = SleepBackground,
        primaryContainer = SleepSurfaceAlt,
        onPrimaryContainer = SleepOnSurface,
        secondary = secondary,
        onSecondary = SleepBackground,
        tertiary = secondary,
        onTertiary = SleepBackground,
        background = SleepBackground,
        onBackground = SleepOnSurface,
        surface = SleepSurface,
        onSurface = SleepOnSurface,
        surfaceVariant = SleepSurfaceAlt,
        onSurfaceVariant = SleepOnSurfaceMuted,
        outline = SleepOutline,
        error = SleepError,
        onError = SleepBackground,
        errorContainer = SleepSurfaceAlt,
        onErrorContainer = SleepOnSurface,
    )
}

@Composable
fun AfterSleepTheme(
    monochrome: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = baseColors(monochrome),
        typography = SleepTypography,
        content = content,
    )
}
