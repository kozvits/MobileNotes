package com.mobilenotes.app.domain.usecase

import com.mobilenotes.app.domain.model.TagCount
import com.mobilenotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetAllTags @Inject constructor(
    private val noteRepository: NoteRepository
) {
    operator fun invoke(): Flow<List<TagCount>> {
        return noteRepository.getAllTags()
    }
}
