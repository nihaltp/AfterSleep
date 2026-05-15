package com.nihaltp.aftersleep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nihaltp.aftersleep.ui.AfterSleepApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val context = LocalContext.current
            val app = context.applicationContext as AfterSleepApplication
            val settingsRepository = app.container.settingsRepository
            val settings by settingsRepository.settingsFlow.collectAsStateWithLifecycle(
                initialValue = com.nihaltp.aftersleep.data.model.UserSettings(),
            )

            LaunchedEffect(settings.keepScreenDimEnabled) {
                window.attributes =
                    window.attributes.apply {
                        screenBrightness = if (settings.keepScreenDimEnabled) 0.08f else android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    }
            }

            AfterSleepApp(app = app)
        }
    }
}
