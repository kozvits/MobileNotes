package com.mobilenotes.app.domain.repository

import com.mobilenotes.app.domain.model.Folder
import com.mobilenotes.app.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    fun getAllFolders(): Flow<List<Folder>>
    fun getFolderById(id: String): Flow<Folder?>
    fun getFoldersWithNoteCount(): Flow<List<Pair<Folder, Int>>>
    suspend fun createFolder(folder: Folder): Result<Folder>
    suspend fun deleteFolder(id: String): Result<Unit>
    suspend fun renameFolder(id: String, name: String): Result<Unit>
}
