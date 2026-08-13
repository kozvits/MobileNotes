package com.mobilenotes.app.data.repository

import com.mobilenotes.app.data.local.datastore.UserPreferences
import com.mobilenotes.app.domain.model.UserSettings
import com.mobilenotes.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val userPreferences: UserPreferences
) : SettingsRepository {

    override fun getSettings(): Flow<UserSettings> = userPreferences.run {
        kotlinx.coroutines.flow.combine(
            isGridView, defaultPaperType, sortOrder, useDynamicColor, appLockEnabled
        ) { grid, paper, sort, dyn, lock ->
            UserSettings(
                isGridView = grid,
                defaultPaperType = paper,
                sortOrder = sort,
                useDynamicColor = dyn,
                appLockEnabled = lock
            )
        }
    }

    override suspend fun setGridView(enabled: Boolean) = userPreferences.setGridView(enabled)
    override suspend fun setDefaultPaperType(type: String) = userPreferences.setDefaultPaperType(type)
    override suspend fun setSortOrder(order: Int) = userPreferences.setSortOrder(order)
    override suspend fun setDynamicColor(enabled: Boolean) = userPreferences.setDynamicColor(enabled)
    override suspend fun setAppLockEnabled(enabled: Boolean) = userPreferences.setAppLockEnabled(enabled)
}
