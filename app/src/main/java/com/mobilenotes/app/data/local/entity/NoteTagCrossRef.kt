package com.mobilenotes.app.data.local.entity

import androidx.room.Entity

@Entity(tableName = "note_tags", primaryKeys = ["noteId", "tagId"])
data class NoteTagCrossRef(
    val noteId: String,
    val tagId: String
)
