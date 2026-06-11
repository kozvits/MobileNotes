package com.mobilenotes.app.domain.usecase

import com.mobilenotes.app.domain.repository.NoteRepository
import javax.inject.Inject

class RestoreNote @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(noteId: String) {
        noteRepository.restoreNote(noteId)
    }
}
