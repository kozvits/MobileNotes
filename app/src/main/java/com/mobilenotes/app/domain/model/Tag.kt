package com.mobilenotes.app.domain.model

data class Tag(
    val id: String,
    val name: String,
    val color: String? = null,
    val emoji: String? = null
)
