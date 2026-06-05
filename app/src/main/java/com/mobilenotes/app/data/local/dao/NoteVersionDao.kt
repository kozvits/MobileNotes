package com.mobilenotes.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobilenotes.app.data.local.entity.NoteVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteVersionDao {

    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY versionNumber DESC LIMIT 10")
    fun getVersionsForNote(noteId: String): Flow<List<NoteVersionEntity>>

    @Query("SELECT COALESCE(MAX(versionNumber), 0) FROM note_versions WHERE noteId = :noteId")
    suspend fun getLatestVersionNumber(noteId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: NoteVersionEntity)

    @Query("DELETE FROM note_versions WHERE noteId = :noteId AND id NOT IN (SELECT id FROM note_versions WHERE noteId = :noteId ORDER BY versionNumber DESC LIMIT 10)")
    suspend fun trimVersions(noteId: String)
}
