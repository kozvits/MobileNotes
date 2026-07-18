package com.mobilenotes.app.presentation.drawing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobilenotes.app.domain.model.PaperType
import com.mobilenotes.app.presentation.components.DrawingCanvas
import com.mobilenotes.app.presentation.components.DrawingColorBar
import com.mobilenotes.app.presentation.components.DrawingColors
import com.mobilenotes.app.presentation.components.DrawingMode
import com.mobilenotes.app.presentation.components.HighlighterColors
import com.mobilenotes.app.presentation.components.PaperTypeSelector
import com.mobilenotes.app.presentation.components.StrokeData
import com.mobilenotes.app.presentation.components.StrokeWidthSelector
import com.mobilenotes.app.presentation.components.StrokeWidths

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandwritingNoteScreen(
    noteId: String? = null,
    onNavigateBack: () -> Unit = {},
    viewModel: HandwritingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Local state for real-time drawing (strokes are committed via viewModel)
    val localStrokes = remember { mutableStateListOf<StrokeData>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    // Sync loaded strokes from ViewModel into local state
    LaunchedEffect(uiState.strokes) {
        if (uiState.strokes.isNotEmpty() && localStrokes.isEmpty()) {
            localStrokes.clear()
            localStrokes.addAll(uiState.strokes)
        }
    }

    // Filter strokes by visible layers for rendering
    val visibleStrokes = remember(localStrokes.toList(), uiState.layers) {
        val visible = uiState.layers.indices.filter { uiState.layers[it].visible }.toSet()
        localStrokes.filter { it.layerIndex in visible }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isNew) "Handwriting Note" else "Edit Handwriting",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        // Commit any remaining strokes
                        if (currentPoints.size > 1) {
                            val stroke = StrokeData(
                                points = currentPoints.toList(),
                                color = uiState.currentColor,
                                strokeWidth = uiState.currentStrokeWidth
                            )
                            localStrokes.add(stroke)
                            viewModel.addStroke(stroke)
                        }
                        viewModel.saveOnExit()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        Text("Saving...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(onClick = { viewModel.save() }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Title field ──
            BasicTextField(
                value = uiState.title,
                onValueChange = { viewModel.onTitleChanged(it) },
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        if (uiState.title.isEmpty()) {
                            Text(
                                "Note title (optional)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Divider()

            // ── Drawing canvas ──
            DrawingCanvas(
                strokes = visibleStrokes,
                currentPoints = currentPoints,
                currentColor = when (uiState.drawingMode) {
                    DrawingMode.ERASER -> Color.White
                    DrawingMode.HIGHLIGHTER -> uiState.currentColor.copy(alpha = 0.45f)
                    DrawingMode.PEN -> uiState.currentColor
                    DrawingMode.SELECT -> Color.Transparent
                },
                currentStrokeWidth = uiState.currentStrokeWidth,
                drawingMode = uiState.drawingMode,
                paperType = uiState.paperType,
                selectedIndices = uiState.selectedIndices,
                selectionRect = uiState.selectionRect,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp),
                onStrokeStart = { pos ->
                    currentPoints = listOf(pos)
                },
                onStrokeMove = { pos ->
                    currentPoints = currentPoints + pos
                },
                onStrokeEnd = {
                    if (currentPoints.size > 1) {
                        val stroke = StrokeData(
                            points = currentPoints.toList(),
                            color = uiState.currentColor,
                            strokeWidth = uiState.currentStrokeWidth
                        )
                        localStrokes.add(stroke)
                        viewModel.addStroke(stroke)
                    }
                    currentPoints = emptyList()
                },
                onSelectionUpdate = { viewModel.onSelectionUpdate(it) },
                onSelectionEnd = { viewModel.onSelectionEnd() }
            )

            Divider()

            // ── Layer selector bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                uiState.layers.forEachIndexed { index, layer ->
                    val isActive = index == uiState.activeLayerIndex
                    val strokeCount = uiState.strokes.count { it.layerIndex == index }
                    FilledTonalButton(
                        onClick = { viewModel.onActiveLayerChanged(index) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isActive)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.height(30.dp),
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                        enabled = layer.visible
                    ) {
                        Icon(
                            Icons.Default.VisibilityOff,
                            contentDescription = if (layer.visible) "Visible" else "Hidden",
                            modifier = Modifier.size(14.dp),
                            tint = if (layer.visible)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        if (strokeCount > 0) {
                            Spacer(Modifier.width(2.dp))
                            Text(
                                "($strokeCount)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    // Toggle visibility
                    FilledTonalIconButton(
                        onClick = { viewModel.toggleLayerVisibility(index) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (layer.visible) Icons.Default.VisibilityOff else Icons.Default.VisibilityOff,
                            contentDescription = if (layer.visible) "Hide layer" else "Show layer",
                            modifier = Modifier.size(14.dp),
                            tint = if (layer.visible)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // ── Toolbar ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Row 1: Colors + Tools
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DrawingColorBar(
                        colors = if (uiState.drawingMode == DrawingMode.HIGHLIGHTER) HighlighterColors else DrawingColors,
                        currentColor = uiState.currentColor,
                        onColorSelected = { viewModel.onColorChanged(it) },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(8.dp))

                    // Undo
                    FilledTonalIconButton(
                        onClick = {
                            if (localStrokes.isNotEmpty()) {
                                localStrokes.removeLast()
                            }
                            viewModel.undoLastStroke()
                        },
                        enabled = localStrokes.isNotEmpty(),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo",
                            modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.width(4.dp))

                    // Clear all
                    FilledTonalIconButton(
                        onClick = {
                            localStrokes.clear()
                            viewModel.clearAll()
                        },
                        enabled = localStrokes.isNotEmpty(),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear",
                            modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.width(4.dp))

                    // Tool switch: Pen / Highlighter / Eraser / Select
                    val toolIcon = when (uiState.drawingMode) {
                        DrawingMode.PEN -> Icons.Default.Brush
                        DrawingMode.HIGHLIGHTER -> Icons.Default.ColorLens
                        DrawingMode.ERASER -> Icons.Default.Close
                        DrawingMode.SELECT -> Icons.Default.NearMe
                    }
                    val toolDesc = when (uiState.drawingMode) {
                        DrawingMode.PEN -> "Pen"
                        DrawingMode.HIGHLIGHTER -> "Highlighter"
                        DrawingMode.ERASER -> "Eraser"
                        DrawingMode.SELECT -> "Select"
                    }
                    FilledTonalIconButton(
                        onClick = {
                            val next = when (uiState.drawingMode) {
                                DrawingMode.PEN -> DrawingMode.HIGHLIGHTER
                                DrawingMode.HIGHLIGHTER -> DrawingMode.ERASER
                                DrawingMode.ERASER -> DrawingMode.SELECT
                                DrawingMode.SELECT -> DrawingMode.PEN
                            }
                            viewModel.setDrawingMode(next)
                            // Auto-select appropriate color when switching
                            if (next == DrawingMode.HIGHLIGHTER) {
                                viewModel.onColorChanged(HighlighterColors.first())
                            } else if (next == DrawingMode.PEN && uiState.currentColor.alpha < 0.5f) {
                                viewModel.onColorChanged(DrawingColors.first())
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            toolIcon,
                            contentDescription = toolDesc,
                            modifier = Modifier.size(20.dp),
                            tint = when (uiState.drawingMode) {
                                DrawingMode.ERASER -> MaterialTheme.colorScheme.error
                                DrawingMode.SELECT -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                // Selection actions bar (shown when strokes are selected)
                if (uiState.selectedIndices.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${uiState.selectedIndices.size} stroke(s) selected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalIconButton(
                                onClick = { viewModel.deleteSelected() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteForever,
                                    contentDescription = "Delete selected",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            FilledTonalIconButton(
                                onClick = { viewModel.clearSelection() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear selection",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Row 2: Stroke width + Paper type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StrokeWidthSelector(
                        widths = StrokeWidths,
                        currentWidth = uiState.currentStrokeWidth,
                        onWidthSelected = { viewModel.onStrokeWidthChanged(it) }
                    )

                    Spacer(Modifier.width(12.dp))

                    PaperTypeSelector(
                        current = uiState.paperType,
                        onSelected = { viewModel.onPaperTypeChanged(it) }
                    )
                }
            }
        }
    }
}
