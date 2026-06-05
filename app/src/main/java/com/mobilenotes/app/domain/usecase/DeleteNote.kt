package com.mobilenotes.app.domain.usecase

import com.mobilenotes.app.domain.model.Result
import com.mobilenotes.app.domain.repository.NoteRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteNote @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.deleteNote(id)
}
