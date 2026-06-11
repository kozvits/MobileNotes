package com.mobilenotes.app.domain.repository

import com.mobilenotes.app.domain.model.Note
import com.mobilenotes.app.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    fun getNoteById(id: String): Flow<Note?>
    fun getNotesByFolder(folderId: String): Flow<List<Note>>
    fun getStarredNotes(): Flow<List<Note>>
    fun getTrashedNotes(): Flow<List<Note>>
    fun searchNotes(query: String): Flow<List<Note>>
    fun getAllTags(): Flow<List<TagCount>>
    suspend fun createNote(note: Note): Result<Note>
    suspend fun updateNote(note: Note): Result<Note>
    suspend fun deleteNote(id: String): Result<Unit>
    suspend fun permanentlyDeleteNote(id: String): Result<Unit>
    suspend fun restoreNote(id: String): Result<Unit>
    suspend fun togglePin(id: String): Result<Unit>
    suspend fun toggleStar(id: String): Result<Unit>
}
