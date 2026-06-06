package com.mobilenotes.app.data.local.entity

import androidx.room.ColumnInfo

data class FolderWithNoteCount(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val iconEmoji: String? = null,
    @ColumnInfo(name = "noteCount")
    val noteCount: Int = 0
)
