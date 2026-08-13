package com.mobilenotes.app.data.repository

import com.mobilenotes.app.data.local.dao.NoteTagDao
import com.mobilenotes.app.data.local.dao.TagDao
import com.mobilenotes.app.data.local.entity.NoteTagCrossRef
import com.mobilenotes.app.data.local.entity.TagEntity
import com.mobilenotes.app.domain.model.Tag
import com.mobilenotes.app.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
    private val noteTagDao: NoteTagDao
) : TagRepository {

    override fun getAllTags(): Flow<List<Tag>> =
        tagDao.getAllTags().map { list -> list.map { it.toDomain() } }

    override suspend fun createTag(tag: Tag): Tag {
        val entity = tag.toEntity()
        tagDao.insertTag(entity)
        return tag
    }

    override suspend fun updateTag(tag: Tag) {
        tagDao.insertTag(tag.toEntity())
    }

    override suspend fun deleteTag(tag: Tag) {
        tagDao.deleteTag(tag.toEntity())
    }

    override suspend fun assignTagToNote(noteId: String, tagId: String) {
        noteTagDao.insertCrossRef(NoteTagCrossRef(noteId, tagId))
    }

    override suspend fun removeTagFromNote(noteId: String, tagId: String) {
        noteTagDao.removeTagFromNote(noteId, tagId)
    }

    override suspend fun getTagByName(name: String): Tag? =
        tagDao.getTagByName(name)?.toDomain()

    private fun TagEntity.toDomain() = Tag(id = id, name = name, color = color, emoji = emoji)
    private fun Tag.toEntity() = TagEntity(id = id, name = name, color = color, emoji = emoji)
}
