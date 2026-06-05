package com.mobilenotes.app.domain.usecase

import com.mobilenotes.app.domain.model.Result
import com.mobilenotes.app.domain.repository.NoteRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToggleStar @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.toggleStar(id)
}
