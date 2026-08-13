package com.mobilenotes.app.domain.usecase

import com.mobilenotes.app.domain.model.UserSettings
import com.mobilenotes.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSettings @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<UserSettings> = repository.getSettings()
}
