package com.nihaltp.aftersleep.media

import android.content.Context
import android.content.Intent
import android.os.DeadObjectException
import android.util.Log
import androidx.core.content.ContextCompat
import com.nihaltp.aftersleep.BuildConfig
import com.nihaltp.aftersleep.data.ActiveSessionRepository
import com.nihaltp.aftersleep.data.SettingsRepository
import com.nihaltp.aftersleep.data.model.MediaSessionSnapshot

class MediaPlaybackOrchestrator(
    private val context: Context,
    private val activeSessionRepository: ActiveSessionRepository,
    private val activeSessionReader: ActiveMediaSessionReader,
    private val settingsRepository: SettingsRepository,
) {
    fun refreshActiveSessions(): List<MediaSessionSnapshot> {
        val snapshots = activeSessionReader.readActiveSessions()
        activeSessionRepository.update(snapshots)
        return snapshots
    }

    fun getBestSession(): MediaSessionSnapshot? =
        activeSessionReader.getBestSession().also { session ->
            if (session != null) {
                activeSessionRepository.update(listOf(session))
            }
        }

    fun pause(session: MediaSessionSnapshot? = getBestSession()): Boolean {
        val controller = findController(session) ?: return false
        return safeAction("pause", session?.packageName) { controller.transportControls.pause() }
    }

    fun play(session: MediaSessionSnapshot? = getBestSession()): Boolean {
        val controller = findController(session) ?: return false
        return safeAction("play", session?.packageName) { controller.transportControls.play() }
    }

    fun stop(session: MediaSessionSnapshot? = getBestSession()): Boolean {
        val controller = findController(session) ?: return false
        return safeAction("stop", session?.packageName) { controller.transportControls.stop() }
    }

    fun openPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return runCatching {
            ContextCompat.startActivity(context, launchIntent, null)
            true
        }.getOrDefault(false)
    }

    fun maybeOpenLastUsedApp(session: MediaSessionSnapshot?): Boolean {
        val packageName = session?.packageName ?: return false
        return openPackage(packageName)
    }

    fun getController(session: MediaSessionSnapshot?): android.media.session.MediaController? =
        findController(
            session,
        )

    private fun findController(session: MediaSessionSnapshot?): android.media.session.MediaController? {
        val bestSession = session ?: getBestSession() ?: return null
        return runCatching {
            activeSessionReader.getController(
                bestSession.packageName,
            )
        }.getOrNull()
    }

    private fun safeAction(
        name: String,
        packageName: String?,
        block: () -> Unit,
    ): Boolean {
        return try {
            block()
            true
        } catch (deadObject: DeadObjectException) {
            log("$name failed for $packageName: dead controller")
            false
        } catch (throwable: Throwable) {
            log("$name failed for $packageName: ${throwable.javaClass.simpleName}")
            false
        }
    }

    private fun log(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d("AfterSleepMedia", message)
        }
    }
}
