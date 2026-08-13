package com.mobilenotes.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed user preferences. Persists UI/settings so they survive
 * process death (previously isGridView was held in-memory only in HomeViewModel).
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    private object Keys {
        val IS_GRID_VIEW = booleanPreferencesKey("is_grid_view")
        val DEFAULT_PAPER_TYPE = stringPreferencesKey("default_paper_type")
        val SORT_ORDER = intPreferencesKey("sort_order")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    }

    val isGridView: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_GRID_VIEW] ?: false }
    val defaultPaperType: Flow<String> =
        context.dataStore.data.map { it[Keys.DEFAULT_PAPER_TYPE] ?: "GRID" }
    val sortOrder: Flow<Int> = context.dataStore.data.map { it[Keys.SORT_ORDER] ?: 0 }
    val useDynamicColor: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.USE_DYNAMIC_COLOR] ?: true }
    val appLockEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.APP_LOCK_ENABLED] ?: false }

    suspend fun setGridView(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_GRID_VIEW] = enabled }
    }

    suspend fun setDefaultPaperType(type: String) {
        context.dataStore.edit { it[Keys.DEFAULT_PAPER_TYPE] = type }
    }

    suspend fun setSortOrder(order: Int) {
        context.dataStore.edit { it[Keys.SORT_ORDER] = order }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.APP_LOCK_ENABLED] = enabled }
    }
}
