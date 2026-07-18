package com.mobilenotes.app.presentation.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mobilenotes.app.domain.model.PaperType
import java.io.File
import java.io.FileOutputStream
import org.json.JSONArray
import org.json.JSONObject

// ═══════════════════════════════════════════
// DATA MODEL
// ═══════════════════════════════════════════

data class StrokeData(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean = false,
    val isHighlighter: Boolean = false
)

private fun StrokeData.toJson(): JSONObject = JSONObject().apply {
    put("c", color.toArgb())
    put("w", strokeWidth.toDouble())
    put("e", isEraser)
    put("h", isHighlighter)
    val pts = JSONArray()
    points.forEach { pt ->
        pts.put(JSONArray().apply {
            put(pt.x.toDouble())
            put(pt.y.toDouble())
        })
    }
    put("p", pts)
}

private fun JSONObject.toStrokeData(): StrokeData {
    val pts = getJSONArray("p")
    val points = mutableListOf<Offset>()
    for (i in 0 until pts.length()) {
        val arr = pts.getJSONArray(i)
        points.add(Offset(arr.getDouble(0).toFloat(), arr.getDouble(1).toFloat()))
    }
    return StrokeData(
        points = points,
        color = Color(getInt("c")),
        strokeWidth = getDouble("w").toFloat(),
        isEraser = optBoolean("e", false),
        isHighlighter = optBoolean("h", false)
    )
}

/** Serialize strokes to JSON string for storage. */
fun serializeStrokes(strokes: List<StrokeData>, paperType: PaperType): String {
    val root = JSONObject().apply {
        put("v", 2)
        put("paper", paperType.name)
        val arr = JSONArray()
        strokes.forEach { arr.put(it.toJson()) }
        put("strokes", arr)
    }
    return "[handwriting/v2]${root.toString()}"
}

/** Deserialize strokes from stored content. Returns null if not valid handwriting data. */
fun deserializeStrokes(content: String): Triple<List<StrokeData>, PaperType, Boolean>? {
    if (!content.startsWith("[handwriting/v2]")) return null
    return try {
        val json = JSONObject(content.removePrefix("[handwriting/v2]"))
        val paper = try {
            PaperType.valueOf(json.optString("paper", "NONE"))
        } catch (_: Exception) { PaperType.NONE }
        val arr = json.getJSONArray("strokes")
        val strokes = mutableListOf<StrokeData>()
        for (i in 0 until arr.length()) {
            strokes.add(arr.getJSONObject(i).toStrokeData())
        }
        Triple(strokes, paper, true)
    } catch (_: Exception) { null }
}

// ═══════════════════════════════════════════
// CONSTANTS
// ═══════════════════════════════════════════

val DrawingColors = listOf(
    Color.Black,
    Color(0xFF37474F), // dark gray
    Color(0xFF5D4037), // brown
    Color(0xFF1976D2), // blue
    Color(0xFF1565C0), // darker blue
    Color(0xFFD32F2F), // red
    Color(0xFFC62828), // darker red
    Color(0xFF388E3C), // green
    Color(0xFFF57C00), // orange
    Color(0xFF7B1FA2), // purple
    Color(0xFF00838F), // teal
    Color(0xFFF9A825), // yellow
)

/** Colors used for highlighter strokes (semi-transparent). */
val HighlighterColors = listOf(
    Color(0x40FFEB3B), // yellow
    Color(0x4081C784), // green
    Color(0x40FF80AB), // pink
    Color(0x4080D8FF), // light blue
    Color(0x40FF8A80), // salmon
)

/** Drawing tool mode. */
enum class DrawingMode { PEN, HIGHLIGHTER, ERASER }

val StrokeWidths = listOf(
    2f to "Fine",
    4f to "Small",
    6f to "Med",
    10f to "Thick",
    16f to "Bold",
)

// ═══════════════════════════════════════════
// UTILITY
// ═══════════════════════════════════════════

private fun List<Offset>.toPath(): Path = Path().apply {
    if (isEmpty()) return@apply
    moveTo(first().x, first().y)
    for (i in 1..lastIndex) {
        lineTo(this@toPath[i].x, this@toPath[i].y)
    }
}

fun renderStrokesToBitmap(
    strokes: List<StrokeData>,
    canvasWidth: Int = 1080,
    canvasHeight: Int = 1440,
    paperType: PaperType = PaperType.NONE
): Bitmap? {
    if (strokes.isEmpty() && paperType == PaperType.NONE) return null

    val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val scaleX = canvasWidth / 400f
    val scaleY = canvasHeight / 400f

    // Draw paper background
    when (paperType) {
        PaperType.GRID -> drawGridOnBitmap(canvas, canvasWidth, canvasHeight, scaleX)
        PaperType.RULED -> drawRuledOnBitmap(canvas, canvasWidth, canvasHeight, scaleY)
        PaperType.DOTTED -> { /* Dotted not rendered on export bitmap */ }
        PaperType.CORNELL -> { /* Cornell not rendered on export bitmap */ }
        PaperType.NONE -> {}
    }

    // Draw highlighter strokes first (behind)
    strokes.forEach { stroke ->
        if (!stroke.isHighlighter || stroke.isEraser) return@forEach

        val paint = Paint().apply {
            color = stroke.color.toArgb()
            strokeWidth = stroke.strokeWidth * ((scaleX + scaleY) / 2f)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
            alpha = 115  // ~0.45 alpha
        }

        if (stroke.points.size > 1) {
            val androidPath = AndroidPath().apply {
                moveTo(stroke.points.first().x * scaleX, stroke.points.first().y * scaleY)
                for (i in 1 until stroke.points.size) {
                    lineTo(stroke.points[i].x * scaleX, stroke.points[i].y * scaleY)
                }
            }
            canvas.drawPath(androidPath, paint)
        }
    }

    // Draw pen strokes
    strokes.forEach { stroke ->
        if (stroke.isEraser || stroke.isHighlighter) return@forEach

        val paint = Paint().apply {
            color = stroke.color.toArgb()
            strokeWidth = stroke.strokeWidth * ((scaleX + scaleY) / 2f)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        if (stroke.points.size > 1) {
            val androidPath = AndroidPath().apply {
                moveTo(stroke.points.first().x * scaleX, stroke.points.first().y * scaleY)
                for (i in 1 until stroke.points.size) {
                    lineTo(stroke.points[i].x * scaleX, stroke.points[i].y * scaleY)
                }
            }
            canvas.drawPath(androidPath, paint)
        }
    }

    return bitmap
}

private fun drawGridOnBitmap(canvas: AndroidCanvas, w: Int, h: Int, scale: Float) {
    val paint = Paint().apply { color = 0xFFB3D4FC.toInt(); strokeWidth = 1f; isAntiAlias = true }
    val cellSize = 48f * scale
    var x = 0f
    while (x < w) {
        canvas.drawLine(x, 0f, x, h.toFloat(), paint)
        x += cellSize
    }
    var y = 0f
    while (y < h) {
        canvas.drawLine(0f, y, w.toFloat(), y, paint)
        y += cellSize
    }
}

private fun drawRuledOnBitmap(canvas: AndroidCanvas, w: Int, h: Int, scale: Float) {
    val marginPaint = Paint().apply { color = 0xFFFF8A95.toInt(); strokeWidth = 2f; isAntiAlias = true }
    val linePaint = Paint().apply { color = 0xFFFFB3BA.toInt(); strokeWidth = 1f; isAntiAlias = true }
    val marginX = 64f * scale
    val lineSpacing = 36f * scale

    canvas.drawLine(marginX, 0f, marginX, h.toFloat(), marginPaint)

    var y = lineSpacing
    while (y < h) {
        canvas.drawLine(0f, y, w.toFloat(), y, linePaint)
        y += lineSpacing
    }
}

// ═══════════════════════════════════════════
// COMPOSABLE: DrawingCanvas (pure canvas)
// ═══════════════════════════════════════════

/**
 * Pure drawing canvas composable that handles pointer input and renders strokes.
 *
 * @param strokes mutable list of completed strokes
 * @param currentPoints currently-being-drawn points
 * @param currentColor active pen color
 * @param currentStrokeWidth active stroke width
 * @param drawingMode active tool: PEN, HIGHLIGHTER, or ERASER
 * @param paperType background pattern
 * @param modifier
 */
@Composable
fun DrawingCanvas(
    strokes: List<StrokeData>,
    currentPoints: List<Offset>,
    currentColor: Color,
    currentStrokeWidth: Float,
    drawingMode: DrawingMode,
    paperType: PaperType,
    modifier: Modifier = Modifier,
    onStrokeStart: ((Offset) -> Unit)? = null,
    onStrokeMove: ((Offset) -> Unit)? = null,
    onStrokeEnd: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
    ) {
        // Paper background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPaperBackground(paperType, size.width, size.height)
        }

        // Strokes canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(true) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitPointerEvent()
                            val pos = down.changes.first().position
                            onStrokeStart?.invoke(pos)

                            while (true) {
                                val move = awaitPointerEvent()
                                val change = move.changes.first()
                                if (change.pressed) {
                                    onStrokeMove?.invoke(change.position)
                                } else {
                                    onStrokeEnd?.invoke()
                                    break
                                }
                            }
                        }
                    }
                }
        ) {
            // Draw highlighter strokes first (behind pen strokes)
            strokes.forEach { stroke ->
                if (stroke.isEraser || !stroke.isHighlighter) return@forEach
                val path = stroke.points.toPath()
                drawPath(
                    path = path,
                    color = stroke.color,
                    alpha = 0.45f,
                    style = Stroke(
                        width = stroke.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
            // Draw pen strokes (non-eraser, non-highlighter)
            strokes.forEach { stroke ->
                if (stroke.isEraser || stroke.isHighlighter) return@forEach
                val path = stroke.points.toPath()
                drawPath(
                    path = path,
                    color = stroke.color,
                    style = Stroke(
                        width = stroke.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Draw current live stroke
            if (currentPoints.size > 1) {
                val liveColor = when (drawingMode) {
                    DrawingMode.ERASER -> Color.White
                    DrawingMode.HIGHLIGHTER -> currentColor.copy(alpha = 0.45f)
                    DrawingMode.PEN -> currentColor
                }
                val path = currentPoints.toPath()
                drawPath(
                    path = path,
                    color = liveColor,
                    style = Stroke(
                        width = currentStrokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
// COMPOSABLE: DrawingControls (color picker + stroke widths)
// ═══════════════════════════════════════════

@Composable
fun DrawingColorBar(
    colors: List<Color>,
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (color == currentColor)
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        else if (color == Color.Black || color == Color(0xFF37474F))
                            Modifier.border(1.dp, Color.Gray, CircleShape)
                        else Modifier
                    )
                    .clickable { onColorSelected(color) },
                contentAlignment = Alignment.Center
            ) {
                if (color == currentColor) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = if (color == Color.Black || color == Color(0xFF37474F) || color == Color(0xFF5D4037))
                            Color.White else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StrokeWidthSelector(
    widths: List<Pair<Float, String>>,
    currentWidth: Float,
    modifier: Modifier = Modifier,
    onWidthSelected: (Float) -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        widths.forEach { (width, label) ->
            FilledTonalButton(
                onClick = { onWidthSelected(width) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (currentWidth == width)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.height(30.dp),
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// ═══════════════════════════════════════════
// COMPOSABLE: PaperTypeSelector
// ═══════════════════════════════════════════

@Composable
fun PaperTypeSelector(
    current: PaperType,
    onSelected: (PaperType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PaperType.entries.forEach { paper ->
            FilledTonalButton(
                onClick = { onSelected(paper) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (paper == current)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.height(30.dp),
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) {
                Text(
                    when (paper) {
                        PaperType.NONE -> "Blank"
                        PaperType.GRID -> "Grid"
                        PaperType.RULED -> "Ruled"
                        PaperType.DOTTED -> "Dotted"
                        PaperType.CORNELL -> "Cornell"
                    },
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
// COMPOSABLE: DrawingDialog (full-featured)
// ═══════════════════════════════════════════

@Composable
fun DrawingDialog(
    onDismiss: () -> Unit,
    onSave: (filePath: String) -> Unit
) {
    val context = LocalContext.current

    var currentColor by remember { mutableStateOf(DrawingColors[0]) }
    var currentStrokeWidth by remember { mutableFloatStateOf(6f) }
    var drawingMode by remember { mutableStateOf(DrawingMode.PEN) }
    var paperType by remember { mutableStateOf(PaperType.NONE) }

    val strokes = remember { mutableStateListOf<StrokeData>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Handwriting", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Canvas
                DrawingCanvas(
                    strokes = strokes.toList(),
                    currentPoints = currentPoints,
                    currentColor = currentColor,
                    currentStrokeWidth = currentStrokeWidth,
                    drawingMode = drawingMode,
                    paperType = paperType,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    onStrokeStart = { pos ->
                        currentPoints = listOf(pos)
                    },
                    onStrokeMove = { pos ->
                        currentPoints = currentPoints + pos
                    },
                    onStrokeEnd = {
                        if (currentPoints.size > 1) {
                            strokes.add(
                                StrokeData(
                                    points = currentPoints.toList(),
                                    color = currentColor,
                                    strokeWidth = currentStrokeWidth,
                                    isEraser = drawingMode == DrawingMode.ERASER,
                                    isHighlighter = drawingMode == DrawingMode.HIGHLIGHTER
                                )
                            )
                        }
                        currentPoints = emptyList()
                    }
                )

                Spacer(Modifier.height(8.dp))

                // Toolbar: color / tools / undo / clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Color palette (switches between pen and highlighter colors)
                    DrawingColorBar(
                        colors = if (drawingMode == DrawingMode.HIGHLIGHTER) HighlighterColors else DrawingColors,
                        currentColor = currentColor,
                        onColorSelected = {
                            currentColor = it
                            val isHighlight = HighlighterColors.contains(it)
                            drawingMode = if (isHighlight) DrawingMode.HIGHLIGHTER else DrawingMode.PEN
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Tool switch: Pen / Highlighter / Eraser
                    val toolIcon = when (drawingMode) {
                        DrawingMode.PEN -> Icons.Default.Brush
                        DrawingMode.HIGHLIGHTER -> Icons.Default.ColorLens
                        DrawingMode.ERASER -> Icons.Default.Close
                    }
                    val toolDesc = when (drawingMode) {
                        DrawingMode.PEN -> "Pen"
                        DrawingMode.HIGHLIGHTER -> "Highlighter"
                        DrawingMode.ERASER -> "Eraser"
                    }
                    FilledTonalIconButton(
                        onClick = {
                            val next = when (drawingMode) {
                                DrawingMode.PEN -> DrawingMode.HIGHLIGHTER
                                DrawingMode.HIGHLIGHTER -> DrawingMode.ERASER
                                DrawingMode.ERASER -> DrawingMode.PEN
                            }
                            drawingMode = next
                            if (next == DrawingMode.HIGHLIGHTER) {
                                currentColor = HighlighterColors.first()
                            } else if (next == DrawingMode.PEN && currentColor.alpha < 0.5f) {
                                currentColor = DrawingColors.first()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            toolIcon,
                            contentDescription = toolDesc,
                            modifier = Modifier.size(20.dp),
                            tint = if (drawingMode == DrawingMode.ERASER)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Stroke width
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Size:", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    StrokeWidthSelector(
                        widths = StrokeWidths,
                        currentWidth = currentStrokeWidth,
                        onWidthSelected = { currentStrokeWidth = it }
                    )
                }

                // Paper type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Paper:", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    PaperTypeSelector(
                        current = paperType,
                        onSelected = { paperType = it }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            strokes.clear()
                            currentPoints = emptyList()
                        },
                        enabled = strokes.isNotEmpty() || currentPoints.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear")
                    }
                    FilledTonalButton(
                        onClick = { if (strokes.isNotEmpty()) strokes.removeLast() },
                        enabled = strokes.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Undo")
                    }
                    Button(
                        onClick = {
                            val allStrokes = strokes.toList()
                            if (allStrokes.isNotEmpty()) {
                                val bitmap = renderStrokesToBitmap(allStrokes, paperType = paperType)
                                if (bitmap != null) {
                                    val drawingsDir = File(context.filesDir, "drawings")
                                    drawingsDir.mkdirs()
                                    val fileName = "drawing_${System.currentTimeMillis()}.png"
                                    val file = File(drawingsDir, fileName)
                                    FileOutputStream(file).use { out ->
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                    }
                                    onSave("drawings/$fileName")
                                }
                            }
                            onDismiss()
                        },
                        enabled = strokes.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NavigateNext, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Insert")
                    }
                }
            }
        }
    }
}
