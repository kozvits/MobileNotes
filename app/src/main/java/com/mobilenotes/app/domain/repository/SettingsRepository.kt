package com.mobilenotes.app.domain.repository

import com.mobilenotes.app.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<UserSettings>
    suspend fun setGridView(enabled: Boolean)
    suspend fun setDefaultPaperType(type: String)
    suspend fun setSortOrder(order: Int)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setAppLockEnabled(enabled: Boolean)
}
