package com.mobilenotes.app.domain.model

/**
 * Types of paper background for handwriting/drawing canvas.
 */
enum class PaperType(val displayName: String) {
    NONE("Blank"),
    GRID("Grid"),
    RULED("Ruled"),
    DOTTED("Dotted"),
    CORNELL("Cornell Notes")
}
