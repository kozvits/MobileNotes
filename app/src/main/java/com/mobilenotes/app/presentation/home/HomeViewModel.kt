package com.mobilenotes.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilenotes.app.domain.model.Folder
import com.mobilenotes.app.domain.model.Note
import com.mobilenotes.app.domain.model.Result
import com.mobilenotes.app.domain.usecase.CreateFolder
import com.mobilenotes.app.domain.usecase.CreateNote
import com.mobilenotes.app.domain.usecase.DeleteFolder
import com.mobilenotes.app.domain.usecase.DeleteNote
import com.mobilenotes.app.domain.usecase.GetAllFolders
import com.mobilenotes.app.domain.usecase.GetAllNotes
import com.mobilenotes.app.domain.usecase.RenameFolder
import com.mobilenotes.app.domain.usecase.TogglePin
import com.mobilenotes.app.domain.usecase.ToggleStar
import com.mobilenotes.app.domain.usecase.UpdateNote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val folders: List<Pair<Folder, Int>> = emptyList(),
    val selectedFolderId: String? = null,
    val isLoading: Boolean = true,
    val isGridView: Boolean = false,
    val searchQuery: String = "",
    val showFavorites: Boolean = false,
    val contextMenuNote: Note? = null,
    val contextMenuFolder: Folder? = null,
    val showCreateFolderDialog: Boolean = false,
    val showRenameFolderDialog: Boolean = false,
    val renameFolderTarget: Folder? = null,
    val showMoveNoteDialog: Boolean = false,
    val moveNoteTarget: Note? = null,
    val showDeleteFolderConfirm: Boolean = false,
    val deleteFolderTarget: Folder? = null,
    val showSettingsDialog: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllNotes: GetAllNotes,
    private val getAllFolders: GetAllFolders,
    private val createNote: CreateNote,
    private val deleteNote: DeleteNote,
    private val togglePin: TogglePin,
    private val toggleStar: ToggleStar,
    private val createFolder: CreateFolder,
    private val deleteFolder: DeleteFolder,
    private val renameFolder: RenameFolder,
    private val updateNote: UpdateNote
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    private val _showFavorites = MutableStateFlow(false)

    /** Emits a note ID when UI should navigate to editor for it */
    private val _navigateToEditor = MutableStateFlow<String?>(null)
    val navigateToEditor: StateFlow<String?> = _navigateToEditor.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                getAllNotes(),
                getAllFolders(),
                _selectedFolderId,
                _showFavorites
            ) { allNotes, folders, selectedId, showFav ->
                val byFolder = if (selectedId != null) {
                    allNotes.filter { it.folderId == selectedId }
                } else {
                    allNotes
                }
                val filtered = if (showFav) byFolder.filter { it.isStarred }
                else byFolder

                val foldersWithCount = folders.map { folder ->
                    folder to allNotes.count { it.folderId == folder.id }
                }

                HomeUiState(
                    notes = filtered,
                    folders = foldersWithCount,
                    selectedFolderId = selectedId,
                    isLoading = false,
                    isGridView = _uiState.value.isGridView,
                    searchQuery = _uiState.value.searchQuery,
                    showFavorites = showFav
                )
            }.collect { state ->
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
                    showSettingsDialog = _uiState.value.showSettingsDialog
                )
            }
        }
    }

    fun onNavigatedToEditor() {
        _navigateToEditor.value = null
    }

    fun toggleViewMode() {
        _uiState.value = _uiState.value.copy(isGridView = !_uiState.value.isGridView)
    }

    fun selectFolder(folderId: String?) {
        _selectedFolderId.value = folderId
    }

    // ---- Navigation / Drawer ----

    fun showAllNotes() {
        _selectedFolderId.value = null
        _showFavorites.value = false
    }

    fun toggleShowFavorites() {
        _showFavorites.value = !_showFavorites.value
        if (_showFavorites.value) _selectedFolderId.value = null
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
            showSettingsDialog = false
        )
    }

    fun dismissAll() {
        dismissContextMenu()
        dismissDialog()
    }
}
