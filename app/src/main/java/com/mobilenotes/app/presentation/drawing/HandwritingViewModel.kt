package com.mobilenotes.app.presentation.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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

data class Layer(
    val name: String,
    val visible: Boolean = true,
    val locked: Boolean = false
)

data class HandwritingUiState(
    val noteId: String? = null,
    val title: String = "",
    val strokes: List<StrokeData> = emptyList(),
    val paperType: PaperType = PaperType.GRID,
    val currentColor: androidx.compose.ui.graphics.Color = DrawingColorsStatic[0],
    val currentStrokeWidth: Float = 6f,
    val drawingMode: DrawingMode = DrawingMode.PEN,
    val selectedIndices: Set<Int> = emptySet(),
    val selectionRect: Rect? = null,
    val layers: List<Layer> = listOf(
        Layer("Layer 1", visible = true),
        Layer("Layer 2", visible = true),
        Layer("Layer 3", visible = false)
    ),
    val activeLayerIndex: Int = 0,
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
                            lastSavedAt = note.updatedAt,
                            activeLayerIndex = 0
                        )
                    } else {
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

    /** Visible strokes only (filtered by layer visibility). */
    val visibleStrokes: List<StrokeData>
        get() {
            val state = _uiState.value
            val visibleLayers = state.layers.indices.filter { state.layers[it].visible }.toSet()
            return state.strokes.filter { it.layerIndex in visibleLayers }
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
        clearSelection()
    }

    fun onStrokeWidthChanged(width: Float) {
        _uiState.value = _uiState.value.copy(currentStrokeWidth = width)
    }

    fun toggleEraser() {
        val current = _uiState.value.drawingMode
        _uiState.value = _uiState.value.copy(
            drawingMode = if (current == DrawingMode.ERASER) DrawingMode.PEN else DrawingMode.ERASER
        )
        clearSelection()
    }

    fun setDrawingMode(mode: DrawingMode) {
        _uiState.value = _uiState.value.copy(drawingMode = mode)
        if (mode != DrawingMode.SELECT) clearSelection()
    }

    fun onPaperTypeChanged(paper: PaperType) {
        _uiState.value = _uiState.value.copy(paperType = paper)
        scheduleAutoSave()
    }

    fun addStroke(stroke: StrokeData) {
        val state = _uiState.value
        val mode = state.drawingMode
        val enhanced = stroke.copy(
            isHighlighter = mode == DrawingMode.HIGHLIGHTER,
            isEraser = mode == DrawingMode.ERASER,
            layerIndex = state.activeLayerIndex
        )
        _uiState.value = state.copy(
            strokes = state.strokes + enhanced
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
        clearSelection()
    }

    // ── Selection ──

    private fun hitTestStrokes(rect: Rect): Set<Int> {
        val selected = mutableSetOf<Int>()
        _uiState.value.strokes.forEachIndexed { index, stroke ->
            if (stroke.points.isEmpty()) return@forEachIndexed
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE
            stroke.points.forEach { pt ->
                if (pt.x < minX) minX = pt.x
                if (pt.y < minY) minY = pt.y
                if (pt.x > maxX) maxX = pt.x
                if (pt.y > maxY) maxY = pt.y
            }
            val strokeRect = Rect(minX, minY, maxX, maxY)
            if (rect.overlaps(strokeRect)) {
                selected.add(index)
            }
        }
        return selected
    }

    fun onSelectionUpdate(rect: Rect) {
        _uiState.value = _uiState.value.copy(
            selectionRect = rect,
            selectedIndices = hitTestStrokes(rect)
        )
    }

    fun onSelectionEnd() {
        _uiState.value = _uiState.value.copy(selectionRect = null)
    }

    fun deleteSelected() {
        val indices = _uiState.value.selectedIndices
        if (indices.isEmpty()) return
        _uiState.value = _uiState.value.copy(
            strokes = _uiState.value.strokes.filterIndexed { i, _ -> i !in indices },
            selectedIndices = emptySet()
        )
        scheduleAutoSave()
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedIndices = emptySet(),
            selectionRect = null
        )
    }

    // ── Layers ──

    fun onActiveLayerChanged(index: Int) {
        if (index in _uiState.value.layers.indices) {
            _uiState.value = _uiState.value.copy(activeLayerIndex = index)
        }
    }

    fun toggleLayerVisibility(index: Int) {
        val state = _uiState.value
        val layers = state.layers.toMutableList()
        if (index in layers.indices) {
            layers[index] = layers[index].copy(visible = !layers[index].visible)
            _uiState.value = state.copy(layers = layers)
            // If toggling off the active layer, switch to first visible layer
            if (index == state.activeLayerIndex && !layers[index].visible) {
                val firstVisible = layers.indexOfFirst { it.visible }
                if (firstVisible >= 0) {
                    _uiState.value = _uiState.value.copy(activeLayerIndex = firstVisible)
                }
            }
        }
    }

    // ── Auto-save ──

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
