package com.mobilenotes.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobilenotes.app.data.local.entity.FolderEntity
import com.mobilenotes.app.data.local.entity.FolderWithNoteCount
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    fun getFolderById(id: String): Flow<FolderEntity?>

    @Query("SELECT * FROM folders WHERE parentId = :parentId ORDER BY name ASC")
    fun getChildFolders(parentId: String): Flow<List<FolderEntity>>

    @Query("""
        SELECT folders.*, 
               (SELECT COUNT(*) FROM notes WHERE notes.folderId = folders.id AND notes.isDeleted = 0) AS noteCount 
        FROM folders 
        ORDER BY name ASC
    """)
    fun getFoldersWithNoteCount(): Flow<List<FolderWithNoteCount>>

    @Query("""
        SELECT folders.*, 
               (SELECT COUNT(*) FROM notes WHERE notes.folderId = folders.id AND notes.isDeleted = 0) AS noteCount 
        FROM folders 
        WHERE folders.id = :id
    """)
    suspend fun getFolderWithNoteCountById(id: String): FolderWithNoteCount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolderById(id: String)

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun renameFolder(id: String, name: String)

    @Query("UPDATE notes SET folderId = NULL WHERE folderId = :folderId")
    suspend fun unassignNotesFromFolder(folderId: String)
}
