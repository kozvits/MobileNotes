package com.mobilenotes.app.domain.repository

import com.mobilenotes.app.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun getAllTags(): Flow<List<Tag>>
    suspend fun createTag(tag: Tag): Tag
    suspend fun updateTag(tag: Tag)
    suspend fun deleteTag(tag: Tag)
    suspend fun assignTagToNote(noteId: String, tagId: String)
    suspend fun removeTagFromNote(noteId: String, tagId: String)
    suspend fun getTagByName(name: String): Tag?
}
