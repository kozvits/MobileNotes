package com.mobilenotes.app.presentation.utils

import android.content.Context
import android.content.Intent
import com.mobilenotes.app.domain.model.Note

/**
 * Share a note as plain text via the system share sheet.
 * Falls back gracefully if no app can handle the intent.
 */
fun shareNote(context: Context, note: Note) {
    val tagLine = if (note.tags.isNotEmpty()) {
        "\n\n#" + note.tags.joinToString(" #") { it.name }
    } else ""
    val text = buildString {
        if (note.title.isNotBlank()) append(note.title).append("\n\n")
        append(note.content)
        append(tagLine)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, note.title.ifBlank { "Note" })
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooser = Intent.createChooser(intent, "Share note").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
