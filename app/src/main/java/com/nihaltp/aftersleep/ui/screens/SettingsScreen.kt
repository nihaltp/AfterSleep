package com.nihaltp.aftersleep.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nihaltp.aftersleep.BuildConfig
import com.nihaltp.aftersleep.R
import com.nihaltp.aftersleep.data.model.UserSettings
import com.nihaltp.aftersleep.ui.components.SectionHeader
import com.nihaltp.aftersleep.ui.components.SettingActionRow
import com.nihaltp.aftersleep.ui.components.SettingToggleRow
import com.nihaltp.aftersleep.ui.components.SimpleNumberDialog
import com.nihaltp.aftersleep.ui.components.SleepCard
import com.nihaltp.aftersleep.ui.components.formatMinutesLabel
import com.nihaltp.aftersleep.ui.theme.SleepOutline
import com.nihaltp.aftersleep.ui.theme.SleepSurfaceAlt
import com.nihaltp.aftersleep.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    settings: UserSettings,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var defaultDelayDialog by rememberSaveable { mutableStateOf(false) }
    var defaultStopDialog by rememberSaveable { mutableStateOf(false) }
    var defaultDelayMinutes by rememberSaveable { mutableStateOf("10") }
    var defaultStopMinutes by rememberSaveable { mutableStateOf("20") }
    val context = LocalContext.current

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SleepCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Settings", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Keep the app quiet, reliable, and tuned for sleep.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SleepCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionHeader(
                    title = "Default timers",
                    subtitle = "These become the starting values on the main screen.",
                )
                SettingActionRow(
                    title = "Default delay",
                    description = formatMinutesLabel(settings.defaultDelayMillis),
                    actionLabel = "Edit",
                    onAction = { defaultDelayDialog = true },
                )
                SettingActionRow(
                    title = "Default stop-after",
                    description = formatMinutesLabel(settings.defaultStopAfterMillis),
                    actionLabel = if (settings.defaultStopAfterMillis == null) "Set" else "Edit",
                    onAction = { defaultStopDialog = true },
                )
            }
        }

        SleepCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionHeader(
                    title = "Playback behavior",
                    subtitle = "Small touches for a softer nighttime experience.",
                )
                SettingToggleRow(
                    title = "Fade-in volume",
                    description = "Gradually raise volume when playback resumes, when the media app supports it.",
                    checked = settings.fadeInVolumeEnabled,
                    onCheckedChange = viewModel::updateFadeInVolume,
                )
                SettingToggleRow(
                    title = "Fade-out volume",
                    description = "Best-effort soft fade before pausing again.",
                    checked = settings.fadeOutVolumeEnabled,
                    onCheckedChange = viewModel::updateFadeOutVolume,
                )
                SettingToggleRow(
                    title = "Keep screen dim",
                    description = "Reduce screen brightness while the app is open at night.",
                    checked = settings.keepScreenDimEnabled,
                    onCheckedChange = viewModel::updateKeepScreenDim,
                )
                SettingToggleRow(
                    title = "Auto-open last media app",
                    description = "Open the last detected media app if resume needs a fallback.",
                    checked = settings.autoOpenLastUsedMediaApp,
                    onCheckedChange = viewModel::updateAutoOpenLastUsedApp,
                )
                SettingToggleRow(
                    title = "Monochrome mode",
                    description = "Reduce accent color for an even quieter look.",
                    checked = settings.monochromeMode,
                    onCheckedChange = viewModel::updateMonochromeMode,
                )
            }
        }

        SleepCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionHeader(
                    title = "Reliability",
                    subtitle = "Helpful shortcuts for notification access and power settings.",
                )
                SettingActionRow(
                    title = "Battery optimization",
                    description = "Guide Android to keep the service alive overnight.",
                    actionLabel = stringResource(R.string.action_open),
                    onAction = onOpenBatteryOptimizationSettings,
                )
                Text(
                    "AfterSleep never uses accessibility automation or third-party APIs.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SleepCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionHeader(
                    title = stringResource(R.string.settings_links),
                    subtitle = stringResource(R.string.settings_links_subtitle),
                )
                SettingActionRow(
                    title = stringResource(R.string.github_repo_title),
                    description = stringResource(R.string.github_repo_desc),
                    actionLabel = stringResource(R.string.action_open),
                    onAction = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nihaltp/AfterSleep"))
                        context.startActivity(intent)
                    },
                )
                SettingActionRow(
                    title = stringResource(R.string.github_issues_title),
                    description = stringResource(R.string.github_issues_desc),
                    actionLabel = stringResource(R.string.action_open),
                    onAction = {
                        val intent =
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nihaltp/AfterSleep/issues"))
                        context.startActivity(intent)
                    },
                )
            }
        }

        // App Info Card
        SleepCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionHeader(
                    title = stringResource(R.string.settings_app_info),
                    subtitle = stringResource(R.string.settings_app_info_subtitle),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Version Name box
                    InfoBox(
                        label = stringResource(R.string.version_title),
                        value = BuildConfig.VERSION_NAME,
                        modifier = Modifier.weight(1f),
                    )
                    // Version Code box
                    InfoBox(
                        label = stringResource(R.string.version_code),
                        value = BuildConfig.VERSION_CODE.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val installSource =
                        remember {
                            try {
                                val info =
                                    context.packageManager
                                        .getInstallSourceInfo(context.packageName)
                                info.installingPackageName ?: "Unknown"
                            } catch (_: Exception) {
                                "Unknown"
                            }
                        }
                    // Install Source box
                    InfoBox(
                        label = stringResource(R.string.installer),
                        value = installSource,
                        modifier = Modifier.weight(1f),
                    )
                }

                SettingActionRow(
                    title = stringResource(R.string.licenses_title),
                    description = stringResource(R.string.licenses_desc),
                    actionLabel = stringResource(R.string.action_open),
                    onAction = onNavigateToLicenses,
                )
            }
        }
    }

    if (defaultDelayDialog) {
        SimpleNumberDialog(
            title = "Default delay",
            value = defaultDelayMinutes,
            helperText = "Enter the default number of minutes before playback resumes.",
            onValueChange = { defaultDelayMinutes = it.filter { char -> char.isDigit() } },
            onDismiss = { defaultDelayDialog = false },
            onConfirm = {
                viewModel.updateDefaultDelay(
                    (defaultDelayMinutes.toLongOrNull() ?: 10L).coerceAtLeast(1L) * 60_000L,
                )
                defaultDelayDialog = false
            },
        )
    }

    if (defaultStopDialog) {
        SimpleNumberDialog(
            title = "Default stop-after",
            value = defaultStopMinutes,
            helperText = "Enter the default number of minutes after playback resumes before it pauses again.",
            onValueChange = { defaultStopMinutes = it.filter { char -> char.isDigit() } },
            onDismiss = { defaultStopDialog = false },
            onConfirm = {
                viewModel.updateDefaultStopAfter(
                    (defaultStopMinutes.toLongOrNull() ?: 20L).coerceAtLeast(1L) * 60_000L,
                )
                defaultStopDialog = false
            },
        )
    }
}

@Composable
private fun InfoBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(SleepSurfaceAlt, shape = RoundedCornerShape(16.dp))
                .border(1.dp, SleepOutline, RoundedCornerShape(16.dp))
                .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
