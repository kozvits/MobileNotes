package com.mobilenotes.app.data.repository

import com.mobilenotes.app.data.local.dao.NoteDao
import com.mobilenotes.app.data.local.dao.NoteTagDao
import com.mobilenotes.app.data.local.dao.NoteVersionDao
import com.mobilenotes.app.data.local.entity.NoteEntity
import com.mobilenotes.app.data.local.entity.NoteTagCrossRef
import com.mobilenotes.app.data.local.entity.NoteVersionEntity
import com.mobilenotes.app.data.local.entity.NoteWithTags
import com.mobilenotes.app.domain.model.Note
import com.mobilenotes.app.domain.model.Result
import com.mobilenotes.app.domain.model.Tag
import com.mobilenotes.app.domain.model.TagCount
import com.mobilenotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

    override fun getAllTags(): Flow<List<TagCount>> =
        noteDao.getAllNotes().map { notesWithTags ->
            notesWithTags
                .filter { !it.note.isDeleted }
                .flatMap { it.tags }
                .groupBy { it.name }
                .map { (name, tags) ->
                    TagCount(
                        name = name,
                        count = tags.size,
                        color = tags.firstOrNull()?.color
                    )
                }
                .sortedByDescending { it.count }
        }

    override suspend fun createNote(note: Note): Result<Note> {
        return try {
            val entity = note.toEntity()
            noteDao.insertNote(entity)
            note.tags.forEach { tag ->
                noteTagDao.insertCrossRef(NoteTagCrossRef(noteId = note.id, tagId = tag.id))
            }
            Result.Success(note)
        } catch (e: Exception) {
            Result.Error("Failed to create note", e)
        }
    }

    override suspend fun updateNote(note: Note): Result<Note> {
        return try {
            val oldEntity = noteDao.getNoteById(note.id).first()
            val oldNote = oldEntity?.toDomain()

            // Preserve fields the caller may not supply (EditorViewModel sends only
            // title/content). Without this, folderId, pin/star, lock, reminder,
            // createdAt and tags were wiped on every save.
            val merged = if (oldNote != null) {
                note.copy(
                    folderId = note.folderId ?: oldNote.folderId,
                    colorHex = note.colorHex ?: oldNote.colorHex,
                    isPinned = note.isPinned || oldNote.isPinned,
                    isStarred = note.isStarred || oldNote.isStarred,
                    isLocked = note.isLocked || oldNote.isLocked,
                    isDeleted = note.isDeleted || oldNote.isDeleted,
                    reminderTimestamp = note.reminderTimestamp ?: oldNote.reminderTimestamp,
                    createdAt = oldNote.createdAt,
                    tags = note.tags.ifEmpty { oldNote.tags }
                )
            } else {
                note
            }

            val entity = merged.toEntity()
            noteDao.updateNote(entity)

            // Keep tag cross-refs in sync (replace set) so re-insert does not duplicate.
            noteTagDao.clearTagsForNote(merged.id)
            merged.tags.forEach { tag ->
                noteTagDao.insertCrossRef(
                    NoteTagCrossRef(noteId = merged.id, tagId = tag.id)
                )
            }

            if (oldNote != null && (oldNote.title != merged.title || oldNote.content != merged.content)) {
                saveVersion(oldNote)
            }

            Result.Success(merged)
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

    private fun NoteWithTags.toDomain(): Note {
        return Note(
            id = note.id,
            title = note.title,
            content = note.content,
            folderId = note.folderId,
            colorHex = note.colorHex,
            isPinned = note.isPinned,
            isStarred = note.isStarred,
            isDeleted = note.isDeleted,
            isLocked = note.isLocked,
            reminderTimestamp = note.reminderTimestamp,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
            syncedAt = note.syncedAt,
            tags = tags.map { Tag(id = it.id, name = it.name, color = it.color, emoji = it.emoji) }
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
