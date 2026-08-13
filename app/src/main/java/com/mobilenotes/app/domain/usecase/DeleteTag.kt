package com.mobilenotes.app.domain.usecase

import com.mobilenotes.app.domain.model.Tag
import com.mobilenotes.app.domain.repository.TagRepository
import javax.inject.Inject

class DeleteTag @Inject constructor(
    private val repository: TagRepository
) {
    suspend operator fun invoke(tag: Tag) = repository.deleteTag(tag)
}
