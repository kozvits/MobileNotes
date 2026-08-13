package com.mobilenotes.app.domain.usecase

import com.mobilenotes.app.domain.model.Tag
import com.mobilenotes.app.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllTagsFull @Inject constructor(
    private val repository: TagRepository
) {
    operator fun invoke(): Flow<List<Tag>> = repository.getAllTags()
}
