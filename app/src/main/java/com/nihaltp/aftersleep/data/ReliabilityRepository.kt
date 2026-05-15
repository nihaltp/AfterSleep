package com.nihaltp.aftersleep.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

data class AppReliabilitySnapshot(
    val packageName: String,
    val successCount: Int,
    val failureCount: Int,
) {
    val isReliable: Boolean get() = successCount >= 2 && successCount >= failureCount
    val label: String
        get() =
            when {
                isReliable -> "reliable"
                failureCount > successCount -> "may fail"
                else -> "mixed"
            }
}

class ReliabilityRepository(private val context: Context) {
    private fun successKey(packageName: String) = intPreferencesKey("reliability_success_${packageName.hashCode()}")

    private fun failureKey(packageName: String) =
        intPreferencesKey(
            "reliability_failure_${packageName.hashCode()}",
        )

    private val lastSuccessfulPackageKey = stringPreferencesKey("last_successful_package")

    suspend fun recordSuccess(packageName: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[successKey(packageName)] = (preferences[successKey(packageName)] ?: 0) + 1
            preferences[lastSuccessfulPackageKey] = packageName
        }
    }

    suspend fun recordFailure(packageName: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[failureKey(packageName)] = (preferences[failureKey(packageName)] ?: 0) + 1
        }
    }

    suspend fun snapshot(packageName: String): AppReliabilitySnapshot {
        val preferences = context.settingsDataStore.data.first()
        return AppReliabilitySnapshot(
            packageName = packageName,
            successCount = preferences[successKey(packageName)] ?: 0,
            failureCount = preferences[failureKey(packageName)] ?: 0,
        )
    }

    suspend fun lastSuccessfulPackage(): String? = context.settingsDataStore.data.first()[lastSuccessfulPackageKey]
}
