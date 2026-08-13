package com.mobilenotes.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilenotes.app.domain.model.PaperType
import com.mobilenotes.app.domain.model.UserSettings
import com.mobilenotes.app.domain.usecase.GetSettings
import com.mobilenotes.app.domain.usecase.UpdateSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getSettings: GetSettings,
    private val updateSettings: UpdateSettings
) : ViewModel() {

    val uiState: StateFlow<UserSettings> = getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    fun setGridView(enabled: Boolean) = viewModelScope.launch { updateSettings(isGridView = enabled) }
    fun setDefaultPaperType(type: PaperType) =
        viewModelScope.launch { updateSettings(defaultPaperType = type.name) }

    fun setSortOrder(order: Int) = viewModelScope.launch { updateSettings(sortOrder = order) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { updateSettings(useDynamicColor = enabled) }
    fun setAppLockEnabled(enabled: Boolean) = viewModelScope.launch { updateSettings(appLockEnabled = enabled) }
}
