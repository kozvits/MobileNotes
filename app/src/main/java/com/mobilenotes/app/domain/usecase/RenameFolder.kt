package com.mobilenotes.app.domain.usecase

import com.mobilenotes.app.domain.model.Result
import com.mobilenotes.app.domain.repository.FolderRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RenameFolder @Inject constructor(
    private val repository: FolderRepository
) {
    suspend operator fun invoke(id: String, name: String): Result<Unit> = repository.renameFolder(id, name)
}
