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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.io.FileOutputStream

/** A single stroke: list of points + style */
private data class StrokeData(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

private val DrawingColors = listOf(
    Color.Black,
    Color(0xFF1976D2),
    Color(0xFFD32F2F),
    Color(0xFF388E3C),
    Color(0xFFF57C00),
    Color(0xFF7B1FA2)
)

private val StrokeWidths = listOf(
    3f to "Thin",
    6f to "Med",
    10f to "Thick"
)

/** Convert a list of offsets to a Compose Path */
private fun List<Offset>.toPath(): Path = Path().apply {
    if (isEmpty()) return@apply
    moveTo(first().x, first().y)
    for (i in 1..lastIndex) {
        lineTo(this@toPath[i].x, this@toPath[i].y)
    }
}

@Composable
fun DrawingDialog(
    onDismiss: () -> Unit,
    onSave: (filePath: String) -> Unit
) {
    val context = LocalContext.current

    var currentColor by remember { mutableStateOf(DrawingColors[0]) }
    var currentStrokeWidth by remember { mutableFloatStateOf(6f) }
    val strokes = remember { mutableStateListOf<StrokeData>() }

    // Current stroke being drawn
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Handwriting",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Drawing canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .pointerInput(true) {
                            awaitPointerEventScope {
                                while (true) {
                                    val down = awaitPointerEvent()
                                    val pos = down.changes.first().position
                                    currentPoints = listOf(pos)

                                    while (true) {
                                        val move = awaitPointerEvent()
                                        val change = move.changes.first()
                                        if (change.pressed) {
                                            currentPoints = currentPoints + change.position
                                        } else {
                                            if (currentPoints.size > 1) {
                                                strokes.add(
                                                    StrokeData(
                                                        points = currentPoints.toList(),
                                                        color = currentColor,
                                                        strokeWidth = currentStrokeWidth
                                                    )
                                                )
                                            }
                                            currentPoints = emptyList()
                                            break
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    val allStrokes = strokes.toList()
                    val livePoints = currentPoints

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Draw completed strokes
                        allStrokes.forEach { stroke ->
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
                        if (livePoints.size > 1) {
                            val path = livePoints.toPath()
                            drawPath(
                                path = path,
                                color = currentColor,
                                style = Stroke(
                                    width = currentStrokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Color picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ColorLens,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DrawingColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (color == currentColor)
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier
                                )
                                .clickable { currentColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (color == currentColor) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (color == Color.Black) Color.White else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Stroke width picker
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Stroke",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StrokeWidths.forEach { (width, label) ->
                        FilledTonalButton(
                            onClick = { currentStrokeWidth = width },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (currentStrokeWidth == width)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { strokes.clear(); currentPoints = emptyList() },
                        enabled = strokes.isNotEmpty() || currentPoints.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
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
                                val bitmap = renderStrokesToBitmap(allStrokes)
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
                        Text("Save")
                    }
                }
            }
        }
    }
}

private fun renderStrokesToBitmap(strokes: List<StrokeData>): Bitmap? {
    if (strokes.isEmpty()) return null

    val width = 1080
    val height = 720
    val scaleX = width / 400f
    val scaleY = height / 400f

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    strokes.forEach { stroke ->
        val paint = Paint().apply {
            color = stroke.color.toArgb()
            strokeWidth = stroke.strokeWidth * scaleX
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
