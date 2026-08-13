package com.mobilenotes.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mobilenotes.app.data.local.entity.NoteEntity
import com.mobilenotes.app.data.local.entity.NoteWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Transaction
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: String): Flow<NoteWithTags?>

    @Transaction
    @Query("SELECT * FROM notes WHERE folderId = :folderId AND isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByFolder(folderId: String): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isStarred = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getStarredNotes(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getTrashedNotes(): Flow<List<NoteWithTags>>

    @Transaction
    @Query(
        """
        SELECT * FROM notes 
        WHERE isDeleted = 0 
        AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, updatedAt DESC
        """
    )
    fun searchNotes(query: String): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isLocked = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getUnlockedNotes(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isLocked = 0 AND reminderTimestamp IS NOT NULL AND reminderTimestamp <= :now ORDER BY reminderTimestamp ASC")
    fun getDueReminders(now: Long): Flow<List<NoteWithTags>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun permanentlyDeleteNote(id: String)

    @Query("UPDATE notes SET isDeleted = :isDeleted WHERE id = :id")
    suspend fun setDeletedStatus(id: String, isDeleted: Boolean)

    @Query("UPDATE notes SET isPinned = NOT isPinned WHERE id = :id")
    suspend fun togglePin(id: String)

    @Query("UPDATE notes SET isStarred = NOT isStarred WHERE id = :id")
    suspend fun toggleStar(id: String)

    @Query("UPDATE notes SET updatedAt = :timestamp WHERE id = :id")
    suspend fun updateTimestamp(id: String, timestamp: Long = System.currentTimeMillis())
}
