package com.mobilenotes.app.data.repository

import com.mobilenotes.app.data.local.dao.FolderDao
import com.mobilenotes.app.data.local.entity.FolderEntity
import com.mobilenotes.app.domain.model.Folder
import com.mobilenotes.app.domain.model.Result
import com.mobilenotes.app.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepositoryImpl @Inject constructor(
    private val folderDao: FolderDao
) : FolderRepository {

    override fun getAllFolders(): Flow<List<Folder>> =
        folderDao.getAllFolders().map { list -> list.map { it.toDomain() } }

    override fun getFolderById(id: String): Flow<Folder?> =
        folderDao.getFolderById(id).map { it?.toDomain() }

    override fun getFoldersWithNoteCount(): Flow<List<Pair<Folder, Int>>> =
        folderDao.getFoldersWithNoteCount().map { list ->
            list.map { it.toDomain() to it.noteCount }
        }

    override suspend fun createFolder(folder: Folder): Result<Folder> {
        return try {
            folderDao.insertFolder(folder.toEntity())
            Result.Success(folder)
        } catch (e: Exception) {
            Result.Error("Failed to create folder", e)
        }
    }

    override suspend fun deleteFolder(id: String): Result<Unit> {
        return try {
            folderDao.unassignNotesFromFolder(id)
            folderDao.deleteFolderById(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to delete folder", e)
        }
    }

    override suspend fun renameFolder(id: String, name: String): Result<Unit> {
        return try {
            folderDao.renameFolder(id, name)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to rename folder", e)
        }
    }

    private fun FolderEntity.toDomain(): Folder = Folder(
        id = id,
        name = name,
        parentId = parentId,
        iconEmoji = iconEmoji
    )

    private fun com.mobilenotes.app.data.local.entity.FolderWithNoteCount.toDomain(): Folder = Folder(
        id = id,
        name = name,
        parentId = parentId,
        iconEmoji = iconEmoji
    )

    private fun Folder.toEntity(): FolderEntity = FolderEntity(
        id = id,
        name = name,
        parentId = parentId,
        iconEmoji = iconEmoji
    )
}
