package com.mobilenotes.app.domain.model

data class Folder(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val iconEmoji: String? = null
)
