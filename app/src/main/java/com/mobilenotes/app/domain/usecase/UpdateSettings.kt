package com.mobilenotes.app.domain.usecase

import com.mobilenotes.app.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateSettings @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(
        isGridView: Boolean? = null,
        defaultPaperType: String? = null,
        sortOrder: Int? = null,
        useDynamicColor: Boolean? = null,
        appLockEnabled: Boolean? = null
    ) {
        isGridView?.let { repository.setGridView(it) }
        defaultPaperType?.let { repository.setDefaultPaperType(it) }
        sortOrder?.let { repository.setSortOrder(it) }
        useDynamicColor?.let { repository.setDynamicColor(it) }
        appLockEnabled?.let { repository.setAppLockEnabled(it) }
    }
}
