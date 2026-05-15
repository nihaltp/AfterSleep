package com.nihaltp.aftersleep

import android.app.Application
import com.nihaltp.aftersleep.data.AppContainer

class AfterSleepApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationHelper.createChannel()
    }
}
