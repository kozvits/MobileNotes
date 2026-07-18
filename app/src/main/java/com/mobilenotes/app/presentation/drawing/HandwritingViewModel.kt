package com.mobilenotes.app.presentation.drawing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilenotes.app.domain.model.Note
import com.mobilenotes.app.domain.model.PaperType
import com.mobilenotes.app.domain.model.Result
import com.mobilenotes.app.domain.usecase.CreateNote
import com.mobilenotes.app.domain.usecase.GetNote
import com.mobilenotes.app.domain.usecase.UpdateNote
import com.mobilenotes.app.presentation.components.DrawingColors
import com.mobilenotes.app.presentation.components.DrawingMode
import com.mobilenotes.app.presentation.components.HighlighterColors
import com.mobilenotes.app.presentation.components.StrokeData
import com.mobilenotes.app.presentation.components.deserializeStrokes
import com.mobilenotes.app.presentation.components.serializeStrokes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HandwritingUiState(
    val noteId: String? = null,
    val title: String = "",
    val strokes: List<StrokeData> = emptyList(),
    val paperType: PaperType = PaperType.GRID,
    val currentColor: androidx.compose.ui.graphics.Color = DrawingColorsStatic[0],
    val currentStrokeWidth: Float = 6f,
    val drawingMode: DrawingMode = DrawingMode.PEN,
    val isNew: Boolean = true,
    val isSaving: Boolean = false,
    val lastSavedAt: Long? = null
)

/** Static copy of DrawingColors for ViewModel use (Compose-free). */
private val DrawingColorsStatic = listOf(
    androidx.compose.ui.graphics.Color.Black,
    androidx.compose.ui.graphics.Color(0xFF37474F),
    androidx.compose.ui.graphics.Color(0xFF5D4037),
    androidx.compose.ui.graphics.Color(0xFF1976D2),
    androidx.compose.ui.graphics.Color(0xFF1565C0),
    androidx.compose.ui.graphics.Color(0xFFD32F2F),
    androidx.compose.ui.graphics.Color(0xFFC62828),
    androidx.compose.ui.graphics.Color(0xFF388E3C),
    androidx.compose.ui.graphics.Color(0xFFF57C00),
    androidx.compose.ui.graphics.Color(0xFF7B1FA2),
    androidx.compose.ui.graphics.Color(0xFF00838F),
    androidx.compose.ui.graphics.Color(0xFFF9A825),
)

/** Static copy of HighlighterColors for ViewModel use (Compose-free). */
private val HighlighterColorsStatic = listOf(
    androidx.compose.ui.graphics.Color(0x40FFEB3B),
    androidx.compose.ui.graphics.Color(0x4081C784),
    androidx.compose.ui.graphics.Color(0x40FF80AB),
    androidx.compose.ui.graphics.Color(0x4080D8FF),
    androidx.compose.ui.graphics.Color(0x40FF8A80),
)

@HiltViewModel
class HandwritingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getNote: GetNote,
    private val createNote: CreateNote,
    private val updateNote: UpdateNote
) : ViewModel() {

    private val noteId: String? = savedStateHandle.get<String>("noteId")
    private var autoSaveJob: Job? = null
    private var isNotePersisted = false

    private val _uiState = MutableStateFlow(HandwritingUiState(noteId = noteId))
    val uiState: StateFlow<HandwritingUiState> = _uiState.asStateFlow()

    init {
        if (noteId != null) {
            loadNote(noteId)
        }
    }

    private fun loadNote(id: String) {
        viewModelScope.launch {
            getNote(id).collect { note ->
                if (note != null) {
                    val parsed = deserializeStrokes(note.content)
                    if (parsed != null) {
                        val (strokes, paper, _) = parsed
                        _uiState.value = HandwritingUiState(
                            noteId = note.id,
                            title = note.title,
                            strokes = strokes,
                            paperType = paper,
                            isNew = false,
                            lastSavedAt = note.updatedAt
                        )
                    } else {
                        // Fallback: treat as empty handwriting note
                        _uiState.value = _uiState.value.copy(
                            noteId = note.id,
                            title = note.title,
                            isNew = false,
                            lastSavedAt = note.updatedAt
                        )
                    }
                    isNotePersisted = true
                }
            }
        }
    }

    fun onTitleChanged(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
        scheduleAutoSave()
    }

    fun onColorChanged(color: androidx.compose.ui.graphics.Color) {
        val isHighlight = HighlighterColorsStatic.contains(color)
        _uiState.value = _uiState.value.copy(
            currentColor = color,
            drawingMode = if (isHighlight) DrawingMode.HIGHLIGHTER else DrawingMode.PEN
        )
    }

    fun onStrokeWidthChanged(width: Float) {
        _uiState.value = _uiState.value.copy(currentStrokeWidth = width)
    }

    fun toggleEraser() {
        val current = _uiState.value.drawingMode
        _uiState.value = _uiState.value.copy(
            drawingMode = if (current == DrawingMode.ERASER) DrawingMode.PEN else DrawingMode.ERASER
        )
    }

    fun setDrawingMode(mode: DrawingMode) {
        _uiState.value = _uiState.value.copy(drawingMode = mode)
    }

    fun onPaperTypeChanged(paper: PaperType) {
        _uiState.value = _uiState.value.copy(paperType = paper)
        scheduleAutoSave()
    }

    fun addStroke(stroke: StrokeData) {
        val mode = _uiState.value.drawingMode
        val enhanced = stroke.copy(
            isHighlighter = mode == DrawingMode.HIGHLIGHTER,
            isEraser = mode == DrawingMode.ERASER
        )
        _uiState.value = _uiState.value.copy(
            strokes = _uiState.value.strokes + enhanced
        )
        scheduleAutoSave()
    }

    fun undoLastStroke() {
        val strokes = _uiState.value.strokes
        if (strokes.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                strokes = strokes.dropLast(1)
            )
        }
    }

    fun clearAll() {
        _uiState.value = _uiState.value.copy(strokes = emptyList())
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

            val content = serializeStrokes(state.strokes, state.paperType)

            if (isNotePersisted && state.noteId != null) {
                val note = Note(
                    id = state.noteId,
                    title = state.title,
                    content = content,
                    updatedAt = System.currentTimeMillis()
                )
                val result = updateNote(note)
                if (result is Result.Success) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        lastSavedAt = System.currentTimeMillis()
                    )
                }
            } else {
                val result = createNote(
                    title = state.title,
                    content = content
                )
                if (result is Result.Success) {
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
