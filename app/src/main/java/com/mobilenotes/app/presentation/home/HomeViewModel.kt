package com.mobilenotes.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilenotes.app.domain.model.Folder
import com.mobilenotes.app.domain.model.Note
import com.mobilenotes.app.domain.model.Result
import com.mobilenotes.app.domain.model.TagCount
import com.mobilenotes.app.domain.usecase.CreateFolder
import com.mobilenotes.app.domain.usecase.CreateNote
import com.mobilenotes.app.domain.usecase.DeleteFolder
import com.mobilenotes.app.domain.usecase.DeleteNote
import com.mobilenotes.app.domain.usecase.DeleteNotePermanently
import com.mobilenotes.app.domain.usecase.GetAllFolders
import com.mobilenotes.app.domain.usecase.GetAllNotes
import com.mobilenotes.app.domain.usecase.RenameFolder
import com.mobilenotes.app.domain.usecase.RestoreNote
import com.mobilenotes.app.domain.usecase.TogglePin
import com.mobilenotes.app.domain.usecase.ToggleStar
import com.mobilenotes.app.domain.usecase.SetNoteLock
import com.mobilenotes.app.domain.usecase.UpdateNote
import com.mobilenotes.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HomeSection { ALL_NOTES, FAVORITES, TAGS, TRASH }

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val folders: List<Pair<Folder, Int>> = emptyList(),
    val selectedFolderId: String? = null,
    val selectedFolder: Folder? = null,
    val isLoading: Boolean = true,
    val isGridView: Boolean = false,
    val currentSection: HomeSection = HomeSection.ALL_NOTES,
    val contextMenuNote: Note? = null,
    val contextMenuFolder: Folder? = null,
    val showCreateFolderDialog: Boolean = false,
    val showRenameFolderDialog: Boolean = false,
    val renameFolderTarget: Folder? = null,
    val showMoveNoteDialog: Boolean = false,
    val moveNoteTarget: Note? = null,
    val showDeleteFolderConfirm: Boolean = false,
    val deleteFolderTarget: Folder? = null,
    val showSettingsDialog: Boolean = false,
    val showRestoreNoteConfirm: Boolean = false,
    val restoreNoteTarget: Note? = null,
    // Tags
    val tags: List<TagCount> = emptyList(),
    val selectedTag: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllNotes: GetAllNotes,
    private val getAllFolders: GetAllFolders,
    private val createNote: CreateNote,
    private val deleteNote: DeleteNote,
    private val deleteNotePermanently: DeleteNotePermanently,
    private val restoreNote: RestoreNote,
    private val togglePin: TogglePin,
    private val toggleStar: ToggleStar,
    private val setNoteLock: SetNoteLock,
    private val createFolder: CreateFolder,
    private val deleteFolder: DeleteFolder,
    private val renameFolder: RenameFolder,
    private val updateNote: UpdateNote,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    private val _currentSection = MutableStateFlow(HomeSection.ALL_NOTES)
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _isGridView = MutableStateFlow(false)

    /** Emits a note ID when UI should navigate to editor for it */
    private val _navigateToEditor = MutableStateFlow<String?>(null)
    val navigateToEditor: StateFlow<String?> = _navigateToEditor.asStateFlow()

    init {
        // Initialise grid-view preference from persistent settings (DataStore).
        viewModelScope.launch {
            settingsRepository.getSettings().collect { _isGridView.value = it.isGridView }
        }

        viewModelScope.launch {
            combine(
                getAllNotes(),
                getAllFolders(),
                _selectedFolderId,
                _currentSection,
                _selectedTag
            ) { allNotes, folders, selectedId, section, tag ->
                val filteredNotes = when (section) {
                    HomeSection.ALL_NOTES -> {
                        allNotes
                            .filter { !it.isDeleted }
                            .let { byFolder ->
                                if (selectedId != null) byFolder.filter { it.folderId == selectedId }
                                else byFolder
                            }
                            .let { byTag ->
                                if (tag != null) byTag.filter { note -> note.tags.any { it.name == tag } }
                                else byTag
                            }
                    }
                    HomeSection.FAVORITES -> {
                        allNotes
                            .filter { it.isStarred && !it.isDeleted }
                            .let { byFolder ->
                                if (selectedId != null) byFolder.filter { it.folderId == selectedId }
                                else byFolder
                            }
                    }
                    HomeSection.TAGS -> {
                        allNotes.filter { !it.isDeleted }
                    }
                    HomeSection.TRASH -> {
                        allNotes.filter { it.isDeleted }
                    }
                }

                val foldersWithCount = folders.map { folder ->
                    folder to allNotes.count { it.folderId == folder.id && !it.isDeleted }
                }

                val selectedFolderObj = if (selectedId != null) folders.find { it.id == selectedId } else null

                val tagCounts = allNotes
                    .filter { !it.isDeleted }
                    .flatMap { it.tags }
                    .groupBy { it.name }
                    .map { (name, tags) ->
                        TagCount(name = name, count = tags.size, color = tags.firstOrNull()?.color)
                    }
                    .sortedByDescending { it.count }

                HomeUiState(
                    notes = filteredNotes,
                    folders = foldersWithCount,
                    selectedFolderId = selectedId,
                    selectedFolder = selectedFolderObj,
                    isLoading = false,
                    currentSection = section,
                    tags = tagCounts,
                    selectedTag = if (section == HomeSection.TAGS) tag else null
                )
            }.combine(_isGridView) { state, isGridView ->
                state.copy(isGridView = isGridView)
            }.collect { state ->
                // Preserve dialog/menu state across updates
                _uiState.value = state.copy(
                    contextMenuNote = _uiState.value.contextMenuNote,
                    contextMenuFolder = _uiState.value.contextMenuFolder,
                    showCreateFolderDialog = _uiState.value.showCreateFolderDialog,
                    showRenameFolderDialog = _uiState.value.showRenameFolderDialog,
                    renameFolderTarget = _uiState.value.renameFolderTarget,
                    showMoveNoteDialog = _uiState.value.showMoveNoteDialog,
                    moveNoteTarget = _uiState.value.moveNoteTarget,
                    showDeleteFolderConfirm = _uiState.value.showDeleteFolderConfirm,
                    deleteFolderTarget = _uiState.value.deleteFolderTarget,
                    showSettingsDialog = _uiState.value.showSettingsDialog,
                    showRestoreNoteConfirm = _uiState.value.showRestoreNoteConfirm,
                    restoreNoteTarget = _uiState.value.restoreNoteTarget
                )
            }
        }
    }

    fun onNavigatedToEditor() {
        _navigateToEditor.value = null
    }

    fun toggleViewMode() {
        viewModelScope.launch {
            val next = !_isGridView.value
            _isGridView.value = next
            settingsRepository.setGridView(next)
        }
    }

    fun selectSection(section: HomeSection) {
        _currentSection.value = section
        if (section != HomeSection.ALL_NOTES) {
            _selectedFolderId.value = null
        }
        if (section != HomeSection.TAGS) {
            _selectedTag.value = null
        }
    }

    fun selectFolder(folderId: String?) {
        _selectedFolderId.value = folderId
        _currentSection.value = HomeSection.ALL_NOTES
    }

    // ---- Tags ----

    fun toggleTagFilter(tagName: String) {
        _selectedTag.value = if (_selectedTag.value == tagName) null else tagName
    }

    // ---- Note creation ----

    fun onCreateNote() {
        viewModelScope.launch {
            val result = createNote(folderId = _selectedFolderId.value)
            if (result is Result.Success) {
                _navigateToEditor.value = result.data.id
            }
        }
    }

    fun onCreateVoiceNote(transcript: String) {
        viewModelScope.launch {
            val result = createNote(
                title = "",
                content = transcript,
                folderId = _selectedFolderId.value
            )
            if (result is Result.Success) {
                _navigateToEditor.value = result.data.id
            }
        }
    }

    fun onCreatePhotoNote(imagePath: String) {
        viewModelScope.launch {
            val result = createNote(
                title = "Photo Note",
                content = "[img:$imagePath]",
                folderId = _selectedFolderId.value
            )
            if (result is Result.Success) {
                _navigateToEditor.value = result.data.id
            }
        }
    }

    fun onDeleteNote(noteId: String) {
        viewModelScope.launch {
            deleteNote(noteId)
        }
        dismissContextMenu()
    }

    fun onDeleteNotePermanently(noteId: String) {
        viewModelScope.launch {
            deleteNotePermanently(noteId)
        }
        dismissContextMenu()
    }

    fun onToggleLock(note: Note) {
        viewModelScope.launch {
            setNoteLock(note.id, !note.isLocked)
        }
        dismissContextMenu()
    }

    fun onRestoreNote(noteId: String) {
        viewModelScope.launch {
            restoreNote(noteId)
        }
        dismissAll()
    }

    fun onTogglePin(noteId: String) {
        viewModelScope.launch {
            togglePin(noteId)
        }
        dismissContextMenu()
    }

    fun onToggleStar(noteId: String) {
        viewModelScope.launch {
            toggleStar(noteId)
        }
        dismissContextMenu()
    }

    // ---- Folder actions ----

    fun onCreateFolder(name: String) {
        viewModelScope.launch {
            createFolder(name = name)
        }
        dismissDialog()
    }

    fun onDeleteFolder(folderId: String) {
        viewModelScope.launch {
            deleteFolder(folderId)
            if (_selectedFolderId.value == folderId) {
                _selectedFolderId.value = null
            }
        }
        dismissAll()
    }

    fun onRenameFolder(folderId: String, newName: String) {
        viewModelScope.launch {
            renameFolder(folderId, newName)
        }
        dismissDialog()
    }

    fun onMoveNoteToFolder(noteId: String, targetFolderId: String?) {
        viewModelScope.launch {
            val currentNote = _uiState.value.notes.find { it.id == noteId }
            if (currentNote != null) {
                updateNote(currentNote.copy(folderId = targetFolderId))
            }
        }
        dismissDialog()
    }

    // ---- Context menu ----

    fun showNoteContextMenu(note: Note) {
        _uiState.value = _uiState.value.copy(
            contextMenuNote = note,
            contextMenuFolder = null
        )
    }

    fun showFolderContextMenu(folder: Folder) {
        _uiState.value = _uiState.value.copy(
            contextMenuFolder = folder,
            contextMenuNote = null
        )
    }

    fun dismissContextMenu() {
        _uiState.value = _uiState.value.copy(
            contextMenuNote = null,
            contextMenuFolder = null
        )
    }

    // ---- Dialogs ----

    fun showCreateFolderDialog() {
        _uiState.value = _uiState.value.copy(showCreateFolderDialog = true)
    }

    fun showRenameFolderDialog(folder: Folder) {
        _uiState.value = _uiState.value.copy(
            showRenameFolderDialog = true,
            renameFolderTarget = folder
        )
        dismissContextMenu()
    }

    fun showMoveNoteDialog(note: Note) {
        _uiState.value = _uiState.value.copy(
            showMoveNoteDialog = true,
            moveNoteTarget = note
        )
        dismissContextMenu()
    }

    fun showDeleteFolderConfirm(folder: Folder) {
        _uiState.value = _uiState.value.copy(
            showDeleteFolderConfirm = true,
            deleteFolderTarget = folder
        )
        dismissContextMenu()
    }

    fun showRestoreNoteConfirm(note: Note) {
        _uiState.value = _uiState.value.copy(
            showRestoreNoteConfirm = true,
            restoreNoteTarget = note
        )
        dismissContextMenu()
    }

    fun showSettingsDialog() {
        _uiState.value = _uiState.value.copy(showSettingsDialog = true)
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateFolderDialog = false,
            showRenameFolderDialog = false,
            renameFolderTarget = null,
            showMoveNoteDialog = false,
            moveNoteTarget = null,
            showDeleteFolderConfirm = false,
            deleteFolderTarget = null,
            showSettingsDialog = false,
            showRestoreNoteConfirm = false,
            restoreNoteTarget = null
        )
    }

    fun dismissAll() {
        dismissContextMenu()
        dismissDialog()
    }
}
