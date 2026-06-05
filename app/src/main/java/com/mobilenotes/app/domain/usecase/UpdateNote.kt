package com.mobilenotes.app.domain.usecase

import com.mobilenotes.app.domain.model.Note
import com.mobilenotes.app.domain.model.Result
import com.mobilenotes.app.domain.repository.NoteRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateNote @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note): Result<Note> {
        val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
        return repository.updateNote(updatedNote)
    }
}
