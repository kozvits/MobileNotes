package com.mobilenotes.app.domain.model

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val folderId: String? = null,
    val colorHex: String? = null,
    val isPinned: Boolean = false,
    val isStarred: Boolean = false,
    val isDeleted: Boolean = false,
    val isLocked: Boolean = false,
    val reminderTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val tags: List<Tag> = emptyList()
)
