package com.nihaltp.aftersleep.ui

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihaltp.aftersleep.AfterSleepApplication
import com.nihaltp.aftersleep.R
import com.nihaltp.aftersleep.ui.model.ScreenRoute
import com.nihaltp.aftersleep.ui.screens.HomeScreen
import com.nihaltp.aftersleep.ui.screens.SettingsScreen
import com.nihaltp.aftersleep.ui.theme.AfterSleepTheme
import com.nihaltp.aftersleep.viewmodel.MainViewModel
import com.nihaltp.aftersleep.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AfterSleepApp(app: AfterSleepApplication) {
    val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.factory(app.container))
    val settingsViewModel: SettingsViewModel =
        viewModel(factory = SettingsViewModel.factory(app.container))
    val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var route by rememberSaveable { mutableStateOf(ScreenRoute.Sleep) }

    LaunchedEffect(Unit) {
        mainViewModel.refreshPermissions()
        mainViewModel.refreshSessions()
    }

    AfterSleepTheme(monochrome = settings.monochromeMode) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("AfterSleep") },
                    navigationIcon = {
                        if (route == ScreenRoute.Licenses) {
                            androidx.compose.material3.IconButton(onClick = { route = ScreenRoute.Settings }) {
                                androidx.compose.material3.Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                )
                            }
                        }
                    },
                    actions = {
                        Row {
                            FilterChip(
                                selected = route == ScreenRoute.Sleep,
                                onClick = { route = ScreenRoute.Sleep },
                                label = { Text("Sleep") },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(
                                selected = route == ScreenRoute.Settings,
                                onClick = { route = ScreenRoute.Settings },
                                label = { Text("Settings") },
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = route,
                    transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                    label = "screen-route",
                ) { screen ->
                    when (screen) {
                        ScreenRoute.Sleep ->
                            HomeScreen(
                                viewModel = mainViewModel,
                                state = mainState,
                                onOpenNotificationSettings = {
                                    context.startActivity(
                                        mainViewModel.requestNotificationSettingsIntent(),
                                    )
                                },
                                onOpenNotificationListenerSettings = {
                                    context.startActivity(
                                        mainViewModel.requestListenerSettingsIntent(),
                                    )
                                },
                                onOpenBatteryOptimizationSettings = {
                                    context.startActivity(
                                        mainViewModel.requestBatteryOptimizationIntent(),
                                    )
                                },
                                onRequestPermissionsRefreshed = { mainViewModel.refreshPermissions() },
                                modifier = Modifier.fillMaxSize().padding(paddingValues),
                            )
                        ScreenRoute.Settings ->
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                settings = settings,
                                onOpenBatteryOptimizationSettings = {
                                    context.startActivity(
                                        mainViewModel.requestBatteryOptimizationIntent(),
                                    )
                                },
                                modifier = Modifier.fillMaxSize().padding(paddingValues),
                                onNavigateToLicenses = { route = ScreenRoute.Licenses },
                            )
                        ScreenRoute.Licenses ->
                            com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer(
                                modifier = Modifier.fillMaxSize().padding(paddingValues),
                            )
                    }
                }
            }
        }
    }
}
