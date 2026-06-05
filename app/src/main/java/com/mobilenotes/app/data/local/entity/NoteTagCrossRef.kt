package com.mobilenotes.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "note_tags",
    primaryKeys = ["noteId", "tagId"],
    indices = [Index("tagId"), Index("noteId")]
)
data class NoteTagCrossRef(
    val noteId: String,
    val tagId: String
)
