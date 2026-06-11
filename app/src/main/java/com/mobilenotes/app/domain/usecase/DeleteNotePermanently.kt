package com.mobilenotes.app.domain.usecase

import com.mobilenotes.app.domain.repository.NoteRepository
import javax.inject.Inject

class DeleteNotePermanently @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(noteId: String) {
        noteRepository.deleteNotePermanently(noteId)
    }
}
