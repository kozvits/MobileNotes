package com.mobilenotes.app.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilenotes.app.domain.model.Tag
import com.mobilenotes.app.domain.usecase.CreateTag
import com.mobilenotes.app.domain.usecase.DeleteTag
import com.mobilenotes.app.domain.usecase.GetAllTagsFull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagPickerViewModel @Inject constructor(
    getAllTags: GetAllTagsFull,
    private val createTag: CreateTag,
    private val deleteTag: DeleteTag
) : ViewModel() {

    val allTags: StateFlow<List<Tag>> = getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Create a tag from raw text (used when typing a new label). */
    suspend fun ensureTag(label: String, color: String? = null, emoji: String? = null): Tag =
        createTag(label, color, emoji)

    fun remove(tag: Tag) = viewModelScope.launch { deleteTag(tag) }
}
