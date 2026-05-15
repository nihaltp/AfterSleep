package com.nihaltp.aftersleep.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.settingsDataStore by preferencesDataStore(name = "settings")
val Context.timerDataStore by preferencesDataStore(name = "timer_state")
