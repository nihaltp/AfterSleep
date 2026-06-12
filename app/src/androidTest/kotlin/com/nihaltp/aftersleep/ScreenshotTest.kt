package com.nihaltp.aftersleep

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nihaltp.aftersleep.data.model.MediaSessionSnapshot
import org.junit.After
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule

@RunWith(AndroidJUnit4::class)
class ScreenshotTest {
    companion object {
        @ClassRule
        @JvmField
        val localeTestRule = LocaleTestRule()
    }

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Force creation of external files directory
        context.getExternalFilesDir(null)
    }

    @After
    fun tearDown() {
    }

    @Test
    fun testTakeScreenshots() {
        // Initialise variable for tracking screenshot count
        var screenshotCount = 1
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        try {
            instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP")
            instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard")
        } catch (e: Exception) {
            // Ignore if shell commands fail
        }

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Hide system status bar and navigation bar dynamically to get only the app screen
        scenario.onActivity { activity ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                activity.setShowWhenLocked(true)
                activity.setTurnScreenOn(true)
            }
            activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            val window = activity.window
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            // Pre-populate mock active session with YouTube
            val app = activity.applicationContext as AfterSleepApplication
            val repository = app.container.activeSessionRepository
            val mockSession = MediaSessionSnapshot(
                packageName = "com.google.android.youtube",
                appLabel = "YouTube",
                title = "Calming Sleep Music 24/7",
                artist = "YouTube Music",
                playbackState = android.media.session.PlaybackState.STATE_PLAYING,
                isActive = true,
                lastUpdatedElapsedRealtime = android.os.SystemClock.elapsedRealtime()
            )
            repository.update(listOf(mockSession))
        }

        // Grant listener permission programmatically via shell if not already granted
        val isGranted = context.packageName in androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context)
        if (!isGranted) {
            val serviceName = "${context.packageName}/com.nihaltp.aftersleep.service.ActiveMediaNotificationListenerService"
            try {
                // Get current listeners
                val pfd = instrumentation.uiAutomation.executeShellCommand("settings get secure enabled_notification_listeners")
                val reader = java.io.BufferedReader(java.io.InputStreamReader(android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd)))
                val current = reader.readLine()?.trim() ?: ""
                reader.close()
                
                val currentList = if (current == "null" || current.isEmpty()) emptyList() else current.split(":")
                if (serviceName !in currentList) {
                    val newList = if (currentList.isEmpty()) serviceName else "$current:$serviceName"
                    instrumentation.uiAutomation.executeShellCommand("settings put secure enabled_notification_listeners $newList")
                    // Wait a moment for system to sync and view model ticker to refresh
                    Thread.sleep(2000)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        composeTestRule.waitForIdle()
        Thread.sleep(2000)

        // Sleep Tab - Capture initial state
        Screengrab.screenshot(screenshotCount.toString())
        screenshotCount++

        // Scroll down on the main screen and capture scrolled state
        composeTestRule.onNodeWithText("Pause now").performScrollTo()
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        Screengrab.screenshot(screenshotCount.toString())
        screenshotCount++

        // Navigate to Settings and capture
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        Screengrab.screenshot(screenshotCount.toString())
        screenshotCount++

        // Scroll down on the settings screen and capture scrolled state
        composeTestRule.onNodeWithText("Battery optimization").performScrollTo()
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        Screengrab.screenshot(screenshotCount.toString())
        screenshotCount++

        scenario.close()

        // Copy screenshots from internal storage to external storage for adb pull
        try {
            val internalDir = java.io.File(context.filesDir.parentFile, "app_screengrab")
            val externalDir = context.getExternalFilesDir("app_screengrab")
            if (externalDir != null) {
                externalDir.deleteRecursively()
            }
            if (internalDir.exists() && externalDir != null) {
                internalDir.copyRecursively(externalDir, overwrite = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
