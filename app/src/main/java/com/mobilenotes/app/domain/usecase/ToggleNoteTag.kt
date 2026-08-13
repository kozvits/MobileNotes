package com.mobilenotes.app.domain.usecase

import com.mobilenotes.app.domain.repository.TagRepository
import javax.inject.Inject

class AssignTagToNote @Inject constructor(
    private val repository: TagRepository
) {
    suspend operator fun invoke(noteId: String, tagId: String) =
        repository.assignTagToNote(noteId, tagId)
}

class RemoveTagFromNote @Inject constructor(
    private val repository: TagRepository
) {
    suspend operator fun invoke(noteId: String, tagId: String) =
        repository.removeTagFromNote(noteId, tagId)
}
