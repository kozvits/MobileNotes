package com.mobilenotes.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobilenotes.app.data.local.entity.NoteTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteTagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: NoteTagCrossRef)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId AND tagId = :tagId")
    suspend fun removeTagFromNote(noteId: String, tagId: String)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun clearTagsForNote(noteId: String)

    @Query("SELECT tagId FROM note_tags WHERE noteId = :noteId")
    fun getTagIdsForNote(noteId: String): Flow<List<String>>

    @Query("SELECT noteId FROM note_tags WHERE tagId = :tagId")
    fun getNoteIdsForTag(tagId: String): Flow<List<String>>
}
