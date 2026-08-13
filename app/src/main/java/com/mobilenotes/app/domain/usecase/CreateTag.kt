package com.mobilenotes.app.domain.usecase

import com.mobilenotes.app.domain.model.Tag
import com.mobilenotes.app.domain.repository.TagRepository
import java.util.UUID
import javax.inject.Inject

class CreateTag @Inject constructor(
    private val repository: TagRepository
) {
    suspend operator fun invoke(name: String, color: String? = null, emoji: String? = null): Tag {
        val clean = name.trim().trimStart('#').replace(Regex("\\s+"), " ")
        val existing = repository.getTagByName(clean)
        return if (existing != null) {
            existing
        } else {
            repository.createTag(
                Tag(id = UUID.randomUUID().toString(), name = clean, color = color, emoji = emoji)
            )
        }
    }
}
