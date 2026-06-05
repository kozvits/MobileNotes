package com.mobilenotes.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilenotes.app.domain.model.Note
import com.mobilenotes.app.domain.usecase.CreateNote
import com.mobilenotes.app.domain.usecase.DeleteNote
import com.mobilenotes.app.domain.usecase.GetAllNotes
import com.mobilenotes.app.domain.usecase.TogglePin
import com.mobilenotes.app.domain.usecase.ToggleStar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true,
    val isGridView: Boolean = false,
    val searchQuery: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllNotes: GetAllNotes,
    private val createNote: CreateNote,
    private val deleteNote: DeleteNote,
    private val togglePin: TogglePin,
    private val toggleStar: ToggleStar
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getAllNotes().collect { notes ->
                _uiState.value = _uiState.value.copy(
                    notes = notes,
                    isLoading = false
                )
            }
        }
    }

    fun toggleViewMode() {
        _uiState.value = _uiState.value.copy(
            isGridView = !_uiState.value.isGridView
        )
    }

    fun onCreateNote() {
        viewModelScope.launch {
            createNote()
        }
    }

    fun onDeleteNote(noteId: String) {
        viewModelScope.launch {
            deleteNote(noteId)
        }
    }

    fun onTogglePin(noteId: String) {
        viewModelScope.launch {
            togglePin(noteId)
        }
    }

    fun onToggleStar(noteId: String) {
        viewModelScope.launch {
            toggleStar(noteId)
        }
    }
}
