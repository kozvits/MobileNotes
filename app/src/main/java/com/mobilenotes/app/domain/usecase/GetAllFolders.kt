package com.mobilenotes.app.domain.usecase

import com.mobilenotes.app.domain.model.Folder
import com.mobilenotes.app.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAllFolders @Inject constructor(
    private val repository: FolderRepository
) {
    operator fun invoke(): Flow<List<Folder>> = repository.getAllFolders()
}
