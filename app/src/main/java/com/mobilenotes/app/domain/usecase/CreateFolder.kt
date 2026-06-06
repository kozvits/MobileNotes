package com.mobilenotes.app.domain.usecase

import com.mobilenotes.app.domain.model.Folder
import com.mobilenotes.app.domain.model.Result
import com.mobilenotes.app.domain.repository.FolderRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreateFolder @Inject constructor(
    private val repository: FolderRepository
) {
    suspend operator fun invoke(
        name: String,
        parentId: String? = null,
        iconEmoji: String? = null
    ): Result<Folder> {
        val folder = Folder(
            id = UUID.randomUUID().toString(),
            name = name,
            parentId = parentId,
            iconEmoji = iconEmoji
        )
        return repository.createFolder(folder)
    }
}
