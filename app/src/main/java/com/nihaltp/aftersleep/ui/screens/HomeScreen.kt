package com.nihaltp.aftersleep.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihaltp.aftersleep.BuildConfig
import com.nihaltp.aftersleep.data.model.PlaybackStage
import com.nihaltp.aftersleep.data.model.TimerPreset
import com.nihaltp.aftersleep.ui.components.AppIcon
import com.nihaltp.aftersleep.ui.components.PrimaryActionButton
import com.nihaltp.aftersleep.ui.components.SectionHeader
import com.nihaltp.aftersleep.ui.components.SimpleNumberDialog
import com.nihaltp.aftersleep.ui.components.SleepCard
import com.nihaltp.aftersleep.ui.components.TimerChip
import com.nihaltp.aftersleep.ui.components.formatDuration
import com.nihaltp.aftersleep.ui.components.formatMinutesLabel
import com.nihaltp.aftersleep.ui.model.MainUiState
import com.nihaltp.aftersleep.ui.theme.SleepBackground
import com.nihaltp.aftersleep.ui.theme.SleepSurfaceAlt
import com.nihaltp.aftersleep.viewmodel.MainViewModel

private val delayPresets =
    listOf(
        TimerPreset("5 min", 5),
        TimerPreset("10 min", 10),
        TimerPreset("15 min", 15),
        TimerPreset("30 min", 30),
        TimerPreset("45 min", 45),
        TimerPreset("1 hour", 60),
    )

private val stopPresets =
    listOf(
        TimerPreset("15 min", 15),
        TimerPreset("30 min", 30),
        TimerPreset("1 hour", 60),
    )

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    state: MainUiState,
    onOpenNotificationSettings: () -> Unit,
    onOpenNotificationListenerSettings: () -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onRequestPermissionsRefreshed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    var selectedDelayMillis by rememberSaveable {
        mutableStateOf(
            state.settings.defaultDelayMillis,
        )
    }
    var selectedStopAfterMillis by rememberSaveable {
        mutableStateOf(state.settings.defaultStopAfterMillis)
    }
    var customDelayDialogOpen by rememberSaveable { mutableStateOf(false) }
    var customStopDialogOpen by rememberSaveable { mutableStateOf(false) }
    var customDelayMinutes by rememberSaveable { mutableStateOf("25") }
    var customStopMinutes by rememberSaveable { mutableStateOf("20") }
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { onRequestPermissionsRefreshed() }

    LaunchedEffect(state.settings) {
        if (selectedDelayMillis <= 0L) {
            selectedDelayMillis = state.settings.defaultDelayMillis
        }
        if (selectedStopAfterMillis == null && state.settings.defaultStopAfterMillis != null) {
            selectedStopAfterMillis = state.settings.defaultStopAfterMillis
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(SleepBackground, SleepSurfaceAlt),
                        ),
                )
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SleepCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = state.greeting,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Silence while falling asleep, audio after sleeping.",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 30.sp,
                )
            }
        }

        val hasAllPermissions =
            state.permissions.notificationPermissionGranted &&
                state.permissions.listenerAccessGranted &&
                state.permissions.batteryOptimizationIgnored

        if (!hasAllPermissions) {
            PermissionsBlock(
                state = state,
                onRequestNotificationPermission = {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                onOpenNotificationSettings = onOpenNotificationSettings,
                onOpenNotificationListenerSettings = onOpenNotificationListenerSettings,
                onOpenBatteryOptimizationSettings = onOpenBatteryOptimizationSettings,
            )
        }

        ActiveSessionCard(state = state)

        TimerStateCard(state = state)

        if (BuildConfig.DEBUG) {
            SleepCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = "Debug tools",
                        subtitle = "Use these to test resume behavior without waiting for long timers.",
                    )
                    androidx.compose.material3.OutlinedButton(
                        onClick = { viewModel.startDebugResumeTest() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Test Resume in 10 Seconds")
                    }
                }
            }
        }

        SleepCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionHeader(
                    title = "Delay before resume",
                    subtitle = "Choose how long playback stays paused before it starts again.",
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    delayPresets.forEach { preset ->
                        TimerChip(
                            label = preset.label,
                            selected = selectedDelayMillis == preset.millis,
                            onClick = { selectedDelayMillis = preset.millis },
                        )
                    }
                    TimerChip(
                        label = "Custom",
                        selected = !delayPresets.any { it.millis == selectedDelayMillis },
                        onClick = { customDelayDialogOpen = true },
                    )
                }
                Text(
                    text = "Selected delay: ${formatMinutesLabel(selectedDelayMillis)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SleepCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionHeader(
                    title = "Optional stop after resume",
                    subtitle = "This timer begins only after playback resumes.",
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TimerChip(
                        label = "Off",
                        selected = selectedStopAfterMillis == null,
                        onClick = { selectedStopAfterMillis = null },
                    )
                    stopPresets.forEach { preset ->
                        TimerChip(
                            label = preset.label,
                            selected = selectedStopAfterMillis == preset.millis,
                            onClick = { selectedStopAfterMillis = preset.millis },
                        )
                    }
                    TimerChip(
                        label = "Custom",
                        selected =
                            selectedStopAfterMillis != null &&
                                stopPresets.none {
                                    it.millis == selectedStopAfterMillis
                                },
                        onClick = { customStopDialogOpen = true },
                    )
                }
                Text(
                    text = "Stop after: ${formatMinutesLabel(selectedStopAfterMillis)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        PrimaryActionButton(
            label = if (state.timerState.active) "Timer active" else "Pause & Start Timer",
            enabled = state.activeSession != null && !state.timerState.active,
            loading = false,
            onClick = {
                hapticFeedback.performHapticFeedback(
                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress,
                )
                viewModel.startDelayTimer(selectedDelayMillis, selectedStopAfterMillis)
            },
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            androidx.compose.material3.OutlinedButton(
                onClick = { viewModel.pauseNow() },
                modifier = Modifier.weight(1f),
            ) {
                Text("Pause now")
            }
            androidx.compose.material3.OutlinedButton(
                onClick = { viewModel.playNow() },
                modifier = Modifier.weight(1f),
            ) {
                Text("Play now")
            }
        }

        if (state.timerState.active) {
            androidx.compose.material3.TextButton(onClick = { viewModel.cancelTimer() }) {
                Text("Cancel timer")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }

    if (customDelayDialogOpen) {
        SimpleNumberDialog(
            title = "Custom delay",
            value = customDelayMinutes,
            helperText = "Enter the number of minutes before playback resumes.",
            onValueChange = { customDelayMinutes = it.filter { char -> char.isDigit() } },
            onDismiss = { customDelayDialogOpen = false },
            onConfirm = {
                selectedDelayMillis =
                    customDelayMinutes.toLongOrNull()?.coerceAtLeast(1)?.times(60_000L)
                        ?: selectedDelayMillis
                customDelayDialogOpen = false
            },
        )
    }

    if (customStopDialogOpen) {
        SimpleNumberDialog(
            title = "Custom stop-after",
            value = customStopMinutes,
            helperText = "Enter the number of minutes after playback resumes before it pauses again.",
            onValueChange = { customStopMinutes = it.filter { char -> char.isDigit() } },
            onDismiss = { customStopDialogOpen = false },
            onConfirm = {
                selectedStopAfterMillis = customStopMinutes.toLongOrNull()?.coerceAtLeast(1)?.times(60_000L)
                customStopDialogOpen = false
            },
        )
    }
}

@Composable
private fun PermissionsBlock(
    state: MainUiState,
    onRequestNotificationPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenNotificationListenerSettings: () -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
) {
    SleepCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader(
                title = "Permissions",
                subtitle = "AfterSleep needs a small set of permissions to keep timers reliable.",
            )
            PermissionRow(
                title = "Notifications",
                description = "Required for the foreground service and quiet playback controls.",
                actionLabel = if (state.permissions.notificationPermissionGranted) "Manage" else "Grant",
                onAction =
                    if (state.permissions.notificationPermissionGranted) {
                        onOpenNotificationSettings
                    } else {
                        onRequestNotificationPermission
                    },
            )
            PermissionRow(
                title = "Media session access",
                description = "Lets AfterSleep detect the active playback app through Android media controls.",
                actionLabel = if (state.permissions.listenerAccessGranted) "Open" else "Enable",
                onAction = onOpenNotificationListenerSettings,
            )
            PermissionRow(
                title = "Battery optimization",
                description = "Optional, but disabling it helps the background timer stay dependable overnight.",
                actionLabel = if (state.permissions.batteryOptimizationIgnored) "Done" else "Guide me",
                onAction = onOpenBatteryOptimizationSettings,
            )
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        androidx.compose.material3.OutlinedButton(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun ActiveSessionCard(state: MainUiState) {
    SleepCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeader(
                title = "Active playback",
                subtitle =
                    when (state.activeSession) {
                        null -> "No active media session detected yet."
                        else -> "Android media controls are available from ${state.activeSession.appLabel}."
                    },
            )

            AnimatedContent(
                targetState = state.activeSession,
                transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                label = "active-session",
            ) { session ->
                if (session == null) {
                    Text(
                        text = "Start playback in a media app, then return here to pause and schedule it.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(packageName = session.packageName, modifier = Modifier.size(56.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                session.appLabel,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(session.displayTitle, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                session.playbackStateLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerStateCard(state: MainUiState) {
    val timerState = state.timerState
    val remaining =
        when {
            timerState.stage == PlaybackStage.WaitingToStart ->
                timerState.delayEndsAtElapsedRealtime - state.nowElapsedRealtime
            timerState.stage == PlaybackStage.WaitingToStop ->
                (timerState.stopEndsAtElapsedRealtime ?: 0L) - state.nowElapsedRealtime
            else -> 0L
        }.coerceAtLeast(0L)
    val stageLabel =
        when (timerState.stage) {
            PlaybackStage.WaitingToStart -> "Waiting to start playback"
            PlaybackStage.WaitingToStop -> "Waiting to stop playback"
            PlaybackStage.Playing -> "Currently playing"
            PlaybackStage.Completed -> "Completed"
            PlaybackStage.Error -> "Failed to resume"
            else -> "Idle"
        }
    val appName = timerState.sessionAppLabel ?: "Selected app"
    val stageDescription =
        when (timerState.stage) {
            PlaybackStage.WaitingToStart -> "$appName resumes in ${formatDuration(remaining)}"
            PlaybackStage.WaitingToStop -> "$appName stops in ${formatDuration(remaining)}"
            PlaybackStage.Playing -> "Playback resumed and the stop timer is now counting down."
            PlaybackStage.Completed -> timerState.lastMessage ?: "Timer finished."
            PlaybackStage.Error -> timerState.lastMessage ?: "Resume failed."
            else -> timerState.lastMessage ?: "No active timer."
        }

    SleepCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Current stage", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                stageLabel,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = stageDescription, color = MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.material3.LinearProgressIndicator(
                progress = {
                    when (timerState.stage) {
                        PlaybackStage.WaitingToStart ->
                            (1f - remaining.toFloat() / timerState.delayMillis.coerceAtLeast(1L).toFloat()).coerceIn(
                                0f,
                                1f,
                            )
                        PlaybackStage.WaitingToStop -> {
                            val stopAfter = timerState.stopAfterMillis ?: 1L
                            (1f - remaining.toFloat() / stopAfter.coerceAtLeast(1L).toFloat()).coerceIn(
                                0f,
                                1f,
                            )
                        }
                        else -> 0f
                    }
                },
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
            if (timerState.sessionAppLabel != null) {
                Text(
                    text = "Locked to ${timerState.sessionAppLabel}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
