package com.mobilenotes.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.mobilenotes.app.domain.model.PaperType

/** Light blue grid lines like traditional squared paper. */
private val GridLineColor = Color(0xFFB3D4FC)
/** Pink horizontal lines like traditional ruled paper. */
private val RuledLineColor = Color(0xFFFFB3BA)
/** Margin line on ruled paper. */
private val RuledMarginColor = Color(0xFFFF8A95)
/** Color for dotted paper dots. */
private val DotColor = Color(0xFFB0BEC5)
/** Color for Cornell layout lines. */
private val CornellLineColor = Color(0xFFB0BEC5)
/** Spacing between grid cells in dp. */
private const val GridCellSizePx = 48f
/** Line spacing in dp for ruled paper. */
private const val RuledLineSpacingPx = 36f
/** Margin offset from the left for ruled paper. */
private const val RuledMarginPx = 64f

/**
 * Draws a paper background pattern on the canvas.
 *
 * @param paperType which pattern to draw
 * @param canvasWidthPx width of the canvas in pixels
 * @param canvasHeightPx height of the canvas in pixels
 */
fun DrawScope.drawPaperBackground(
    paperType: PaperType,
    canvasWidthPx: Float,
    canvasHeightPx: Float
) {
    when (paperType) {
        PaperType.NONE -> { /* nothing */ }
        PaperType.GRID -> drawGridBackground(canvasWidthPx, canvasHeightPx)
        PaperType.RULED -> drawRuledBackground(canvasWidthPx, canvasHeightPx)
        PaperType.DOTTED -> drawDottedBackground(canvasWidthPx, canvasHeightPx)
        PaperType.CORNELL -> drawCornellBackground(canvasWidthPx, canvasHeightPx)
    }
}

private fun DrawScope.drawGridBackground(width: Float, height: Float) {
    // Vertical lines
    var x = 0f
    while (x <= width) {
        drawLine(
            color = GridLineColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = when {
                x % (GridCellSizePx * 5) < 0.1f -> 1.5f // thicker every 5 cells
                else -> 0.5f
            }
        )
        x += GridCellSizePx
    }
    // Horizontal lines
    var y = 0f
    while (y <= height) {
        drawLine(
            color = GridLineColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = when {
                y % (GridCellSizePx * 5) < 0.1f -> 1.5f
                else -> 0.5f
            }
        )
        y += GridCellSizePx
    }
}

private fun DrawScope.drawRuledBackground(width: Float, height: Float) {
    // Vertical margin line on the left
    drawLine(
        color = RuledMarginColor,
        start = Offset(RuledMarginPx, 0f),
        end = Offset(RuledMarginPx, height),
        strokeWidth = 1.5f
    )
    // Horizontal lines
    var y = RuledLineSpacingPx
    while (y <= height) {
        drawLine(
            color = RuledLineColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 0.8f
        )
        y += RuledLineSpacingPx
    }
}

/**
 * Dotted grid pattern — subtle dots at each grid intersection.
 */
private fun DrawScope.drawDottedBackground(width: Float, height: Float) {
    val dotRadius = 1.5f
    val spacing = GridCellSizePx
    var x = spacing
    while (x <= width) {
        var y = spacing
        while (y <= height) {
            drawCircle(
                color = DotColor,
                radius = dotRadius,
                center = Offset(x, y)
            )
            y += spacing
        }
        x += spacing
    }
}

/**
 * Cornell Notes layout.
 *
 * ┌─────────────────────────┬──────────────────────┐
 * │       Header line       │                      │
 * ├────────────┬────────────┤                      │
 * │  Cue / Key │   Notes    │                      │
 * │  column    │   (main)   │                      │
 * │  (30%)     │   (70%)    │                      │
 * │            │            │                      │
 * ├────────────┴────────────┴──────────────────────┤
 * │              Summary (bottom)                  │
 * └────────────────────────────────────────────────┘
 */
private fun DrawScope.drawCornellBackground(width: Float, height: Float) {
    val leftColWidth = width * 0.3f
    val headerHeight = 60f
    val summaryHeight = height * 0.2f
    val notesAreaBottom = height - summaryHeight

    // ── Header area ──
    drawLine(
        color = CornellLineColor,
        start = Offset(0f, headerHeight),
        end = Offset(width, headerHeight),
        strokeWidth = 1f
    )
    // ── Cue column divider (vertical) ──
    drawLine(
        color = CornellLineColor,
        start = Offset(leftColWidth, headerHeight),
        end = Offset(leftColWidth, notesAreaBottom),
        strokeWidth = 1f
    )
    // ── Summary divider (horizontal) ──
    drawLine(
        color = CornellLineColor,
        start = Offset(0f, notesAreaBottom),
        end = Offset(width, notesAreaBottom),
        strokeWidth = 1.5f
    )
    // ── Light guide lines inside the Notes area ──
    var y = RuledLineSpacingPx.coerceAtMost(headerHeight + RuledLineSpacingPx)
    while (y < notesAreaBottom) {
        drawLine(
            color = CornellLineColor.copy(alpha = 0.25f),
            start = Offset(leftColWidth, y),
            end = Offset(width, y),
            strokeWidth = 0.5f
        )
        y += RuledLineSpacingPx
    }
    // ── Light guide lines inside the Cue column ──
    y = RuledLineSpacingPx.coerceAtMost(headerHeight + RuledLineSpacingPx)
    while (y < notesAreaBottom) {
        drawLine(
            color = CornellLineColor.copy(alpha = 0.25f),
            start = Offset(0f, y),
            end = Offset(leftColWidth, y),
            strokeWidth = 0.5f
        )
        y += RuledLineSpacingPx
    }
    // ── Light guide lines inside Summary ──
    y = notesAreaBottom + RuledLineSpacingPx
    while (y < height) {
        drawLine(
            color = CornellLineColor.copy(alpha = 0.25f),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 0.5f
        )
        y += RuledLineSpacingPx
    }
}
