package com.mobilenotes.app.presentation.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilenotes.app.domain.model.Note
import com.mobilenotes.app.domain.usecase.CreateNote
import com.mobilenotes.app.domain.usecase.GetNote
import com.mobilenotes.app.domain.usecase.UpdateNote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorUiState(
    val noteId: String? = null,
    val title: String = "",
    val content: String = "",
    val isNew: Boolean = true,
    val isSaving: Boolean = false,
    val lastSavedAt: Long? = null
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getNote: GetNote,
    private val createNote: CreateNote,
    private val updateNote: UpdateNote
) : ViewModel() {

    private val noteId: String? = savedStateHandle.get<String>("noteId")
    private var autoSaveJob: Job? = null
    private var isNotePersisted = false

    private val _uiState = MutableStateFlow(EditorUiState(noteId = noteId))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        if (noteId != null) {
            loadNote(noteId)
        }
    }

    private fun loadNote(id: String) {
        viewModelScope.launch {
            getNote(id).collect { note ->
                if (note != null) {
                    _uiState.value = EditorUiState(
                        noteId = note.id,
                        title = note.title,
                        content = note.content,
                        isNew = false,
                        lastSavedAt = note.updatedAt
                    )
                    isNotePersisted = true
                }
            }
        }
    }

    fun onTitleChanged(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
        scheduleAutoSave()
    }

    fun onContentChanged(content: String) {
        _uiState.value = _uiState.value.copy(content = content)
        scheduleAutoSave()
    }

    fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(3000)
            save()
        }
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.value = state.copy(isSaving = true)

            if (isNotePersisted && state.noteId != null) {
                val note = Note(
                    id = state.noteId,
                    title = state.title,
                    content = state.content,
                    updatedAt = System.currentTimeMillis()
                )
                val result = updateNote(note)
                if (result is com.mobilenotes.app.domain.model.Result.Success) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        lastSavedAt = System.currentTimeMillis()
                    )
                }
            } else {
                val result = createNote(
                    title = state.title,
                    content = state.content
                )
                if (result is com.mobilenotes.app.domain.model.Result.Success) {
                    isNotePersisted = true
                    _uiState.value = _uiState.value.copy(
                        noteId = result.data.id,
                        isNew = false,
                        isSaving = false,
                        lastSavedAt = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    fun saveOnExit() {
        viewModelScope.launch {
            save()
        }
    }
}
