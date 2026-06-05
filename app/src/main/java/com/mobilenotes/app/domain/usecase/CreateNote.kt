package com.mobilenotes.app.domain.usecase

import com.mobilenotes.app.domain.model.Note
import com.mobilenotes.app.domain.model.Result
import com.mobilenotes.app.domain.repository.NoteRepository
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

@Singleton
class CreateNote @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(
        title: String = "",
        content: String = "",
        folderId: String? = null
    ): Result<Note> {
        val note = Note(
            id = UUID.randomUUID().toString(),
            title = title,
            content = content,
            folderId = folderId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return repository.createNote(note)
    }
}
