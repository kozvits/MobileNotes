package com.mobilenotes.app.data.repository

import com.mobilenotes.app.data.local.dao.NoteDao
import com.mobilenotes.app.data.local.dao.NoteTagDao
import com.mobilenotes.app.data.local.dao.NoteVersionDao
import com.mobilenotes.app.data.local.entity.NoteEntity
import com.mobilenotes.app.data.local.entity.NoteVersionEntity
import com.mobilenotes.app.domain.model.Note
import com.mobilenotes.app.domain.model.Result
import com.mobilenotes.app.domain.model.Tag
import com.mobilenotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val noteTagDao: NoteTagDao,
    private val noteVersionDao: NoteVersionDao
) : NoteRepository {

    override fun getAllNotes(): Flow<List<Note>> =
        noteDao.getAllNotes().map { list -> list.map { it.toDomain() } }

    override fun getNoteById(id: String): Flow<Note?> =
        noteDao.getNoteById(id).map { it?.toDomain() }

    override fun getNotesByFolder(folderId: String): Flow<List<Note>> =
        noteDao.getNotesByFolder(folderId).map { list -> list.map { it.toDomain() } }

    override fun getStarredNotes(): Flow<List<Note>> =
        noteDao.getStarredNotes().map { list -> list.map { it.toDomain() } }

    override fun getTrashedNotes(): Flow<List<Note>> =
        noteDao.getTrashedNotes().map { list -> list.map { it.toDomain() } }

    override fun searchNotes(query: String): Flow<List<Note>> =
        noteDao.searchNotes(query).map { list -> list.map { it.toDomain() } }

    override suspend fun createNote(note: Note): Result<Note> {
        return try {
            val entity = note.toEntity()
            noteDao.insertNote(entity)
            Result.Success(note)
        } catch (e: Exception) {
            Result.Error("Failed to create note", e)
        }
    }

    override suspend fun updateNote(note: Note): Result<Note> {
        return try {
            val oldEntity = noteDao.getNoteById(note.id)
            val oldNote = oldEntity?.toDomain()

            val entity = note.toEntity()
            noteDao.updateNote(entity)

            if (oldNote != null && (oldNote.title != note.title || oldNote.content != note.content)) {
                saveVersion(oldNote)
            }

            Result.Success(note)
        } catch (e: Exception) {
            Result.Error("Failed to update note", e)
        }
    }

    override suspend fun deleteNote(id: String): Result<Unit> {
        return try {
            noteDao.setDeletedStatus(id, true)
            noteDao.updateTimestamp(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to delete note", e)
        }
    }

    override suspend fun permanentlyDeleteNote(id: String): Result<Unit> {
        return try {
            noteDao.permanentlyDeleteNote(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to permanently delete note", e)
        }
    }

    override suspend fun restoreNote(id: String): Result<Unit> {
        return try {
            noteDao.setDeletedStatus(id, false)
            noteDao.updateTimestamp(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to restore note", e)
        }
    }

    override suspend fun togglePin(id: String): Result<Unit> {
        return try {
            noteDao.togglePin(id)
            noteDao.updateTimestamp(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to toggle pin", e)
        }
    }

    override suspend fun toggleStar(id: String): Result<Unit> {
        return try {
            noteDao.toggleStar(id)
            noteDao.updateTimestamp(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to toggle star", e)
        }
    }

    private suspend fun saveVersion(note: Note) {
        try {
            val versionNumber = noteVersionDao.getLatestVersionNumber(note.id) + 1
            val version = NoteVersionEntity(
                noteId = note.id,
                title = note.title,
                content = note.content,
                versionNumber = versionNumber
            )
            noteVersionDao.insertVersion(version)
            noteVersionDao.trimVersions(note.id)
        } catch (_: Exception) {
        }
    }

    private fun NoteEntity.toDomain(): Note {
        return Note(
            id = id,
            title = title,
            content = content,
            folderId = folderId,
            colorHex = colorHex,
            isPinned = isPinned,
            isStarred = isStarred,
            isDeleted = isDeleted,
            isLocked = isLocked,
            reminderTimestamp = reminderTimestamp,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncedAt = syncedAt
        )
    }

    private fun Note.toEntity(): NoteEntity {
        return NoteEntity(
            id = id,
            title = title,
            content = content,
            folderId = folderId,
            colorHex = colorHex,
            isPinned = isPinned,
            isStarred = isStarred,
            isDeleted = isDeleted,
            isLocked = isLocked,
            reminderTimestamp = reminderTimestamp,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncedAt = syncedAt
        )
    }
}
