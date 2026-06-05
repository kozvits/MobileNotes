package com.mobilenotes.app.domain.usecase

import com.mobilenotes.app.domain.model.Note
import com.mobilenotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetNote @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(id: String): Flow<Note?> = repository.getNoteById(id)
}
