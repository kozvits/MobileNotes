package com.mobilenotes.app.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val VIEW_MODE = intPreferencesKey("view_mode")
        private val AUTO_SAVE_INTERVAL = intPreferencesKey("auto_save_interval")
        private val IS_SYNC_ENABLED = booleanPreferencesKey("is_sync_enabled")
        private val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE] ?: "system"
    }

    val viewMode: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[VIEW_MODE] ?: 0
    }

    val isSyncEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_SYNC_ENABLED] ?: false
    }

    val appLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[APP_LOCK_ENABLED] ?: false
    }

    val appLockPin: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[APP_LOCK_PIN]
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[THEME_MODE] = mode }
    }

    suspend fun setViewMode(mode: Int) {
        context.dataStore.edit { prefs -> prefs[VIEW_MODE] = mode }
    }

    suspend fun setSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[IS_SYNC_ENABLED] = enabled }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setAppLockPin(pin: String?) {
        context.dataStore.edit { prefs ->
            if (pin != null) prefs[APP_LOCK_PIN] = pin
            else prefs.remove(APP_LOCK_PIN)
        }
    }
}
