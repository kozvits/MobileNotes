package com.mobilenotes.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val folderId: String? = null,
    val colorHex: String? = null,
    val isPinned: Boolean = false,
    val isStarred: Boolean = false,
    val isDeleted: Boolean = false,
    val isLocked: Boolean = false,
    val reminderTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null
)
