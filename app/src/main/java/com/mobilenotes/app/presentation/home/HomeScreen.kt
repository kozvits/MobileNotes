package com.mobilenotes.app.presentation.home

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import com.mobilenotes.app.domain.model.Folder
import com.mobilenotes.app.domain.model.Note
import com.mobilenotes.app.presentation.utils.authenticateWithBiometrics
import com.mobilenotes.app.presentation.utils.shareNote
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onNavigateToEditor: (String?) -> Unit = {},
    onNavigateToHandwriting: (String?) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // Opens a note, requiring biometric auth first if it is locked.
    fun openNote(note: Note) {
        if (note.isLocked && activity != null) {
            activity.authenticateWithBiometrics(
                onSuccess = { onNavigateToEditor(note.id) },
                onError = { /* keep note closed */ }
            )
        } else {
            onNavigateToEditor(note.id)
        }
    }

    var showFolderPanel by remember { mutableStateOf(true) }
    var showFabMenu by remember { mutableStateOf(false) }

    // ---- Voice recognition launcher ----
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val transcript = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!transcript.isNullOrBlank()) {
                viewModel.onCreateVoiceNote(transcript)
            }
        }
    }

    // ---- Camera launcher ----
    var photoFilePath by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoFilePath?.let { path ->
                viewModel.onCreatePhotoNote(path)
            }
        }
    }

    // ---- Navigate to editor when a note is created ----
    val navigateToNoteId by viewModel.navigateToEditor.collectAsState()
    LaunchedEffect(navigateToNoteId) {
        navigateToNoteId?.let { noteId ->
            onNavigateToEditor(noteId)
            viewModel.onNavigatedToEditor()
        }
    }

    // ================================================================
    // ROOT LAYOUT — Row с анимированной левой панелью папок
    // ================================================================

    Row(modifier = Modifier.fillMaxSize()) {

        // ---- Left panel: folders + navigation ----
        AnimatedVisibility(
            visible = showFolderPanel,
            enter = slideInHorizontally() + expandHorizontally(expandFrom = Alignment.Start),
            exit = slideOutHorizontally() + shrinkHorizontally(shrinkTowards = Alignment.Start)
        ) {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Spacer(Modifier.height(16.dp))

                // App header
                Text(
                    text = "MobileNotes",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                )

                Divider(modifier = Modifier.padding(horizontal = 28.dp))
                Spacer(Modifier.height(8.dp))

                // ===== NAVIGATION SECTION =====
                Text(
                    text = "NAVIGATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("All Notes") },
                    selected = uiState.currentSection == HomeSection.ALL_NOTES,
                    onClick = {
                        viewModel.selectSection(HomeSection.ALL_NOTES)
                        showFolderPanel = false
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text("Favorites") },
                    selected = uiState.currentSection == HomeSection.FAVORITES,
                    onClick = {
                        viewModel.selectSection(HomeSection.FAVORITES)
                        showFolderPanel = false
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Label, contentDescription = null) },
                    label = { Text("Tags") },
                    selected = uiState.currentSection == HomeSection.TAGS,
                    onClick = {
                        viewModel.selectSection(HomeSection.TAGS)
                        showFolderPanel = false
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                    label = { Text("Trash") },
                    selected = uiState.currentSection == HomeSection.TRASH,
                    onClick = {
                        viewModel.selectSection(HomeSection.TRASH)
                        showFolderPanel = false
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(Modifier.height(8.dp))
                Divider(modifier = Modifier.padding(horizontal = 28.dp))
                Spacer(Modifier.height(8.dp))

                // ===== FOLDERS SECTION =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FOLDERS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    IconButton(
                        onClick = { viewModel.showCreateFolderDialog() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New folder",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Folders list
                if (uiState.folders.isEmpty()) {
                    Text(
                        text = "No folders yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(uiState.folders, key = { it.first.id }) { (folder, count) ->
                            NavigationDrawerItem(
                                icon = {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = if (uiState.selectedFolderId == folder.id)
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = folder.name,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (count > 0) {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ) { Text("$count") }
                                        }
                                    }
                                },
                                selected = uiState.selectedFolderId == folder.id,
                                onClick = {
                                    viewModel.selectFolder(folder.id)
                                    showFolderPanel = false
                                },
                                badge = {
                                    IconButton(
                                        onClick = { viewModel.showFolderContextMenu(folder) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DriveFileRenameOutline,
                                            contentDescription = "Folder options",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Divider(modifier = Modifier.padding(horizontal = 28.dp))
                Spacer(Modifier.height(8.dp))

                // ===== SETTINGS =====
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        showFolderPanel = false
                        onNavigateToSettings()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text("About") },
                    selected = false,
                    onClick = {
                        showFolderPanel = false
                        viewModel.showSettingsDialog()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }

        // ================================================================
        // MAIN CONTENT — растягивается на оставшуюся ширину
        // ================================================================
        Scaffold(
            modifier = Modifier.weight(1f),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (uiState.currentSection) {
                                HomeSection.FAVORITES -> "Favorites"
                                HomeSection.TAGS -> "Tags"
                                HomeSection.TRASH -> "Trash"
                                else -> uiState.selectedFolder?.name ?: "MobileNotes"
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { showFolderPanel = !showFolderPanel }) {
                            Icon(
                                if (showFolderPanel) Icons.Default.FolderOpen
                                else Icons.Default.Folder,
                                contentDescription = if (showFolderPanel) "Hide folders" else "Show folders"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                if (uiState.isGridView) Icons.Default.ViewList
                                else Icons.Default.GridView,
                                contentDescription = "Toggle view"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                if (uiState.currentSection != HomeSection.TRASH) {
                    Column(horizontalAlignment = Alignment.End) {
                        if (showFabMenu) {
                            // Text note
                            FilledTonalButton(
                                onClick = {
                                    showFabMenu = false
                                    viewModel.onCreateNote()
                                },
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(Icons.Default.StickyNote2, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Text")
                            }
                            // Handwrite note
                            FilledTonalButton(
                                onClick = {
                                    showFabMenu = false
                                    onNavigateToHandwriting(null)
                                },
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(Icons.Default.Create, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Handwrite")
                            }
                            // Voice note
                            FilledTonalButton(
                                onClick = {
                                    showFabMenu = false
                                    try {
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(
                                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                            )
                                            putExtra(
                                                RecognizerIntent.EXTRA_PROMPT,
                                                "Speak your note..."
                                            )
                                        }
                                        speechLauncher.launch(intent)
                                    } catch (_: ActivityNotFoundException) {
                                        Toast.makeText(
                                            context,
                                            "Speech recognition not available",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Voice")
                            }
                            // Photo note
                            FilledTonalButton(
                                onClick = {
                                    showFabMenu = false
                                    try {
                                        val photosDir = File(context.filesDir, "photos")
                                        photosDir.mkdirs()
                                        val fileName = "photo_${System.currentTimeMillis()}.jpg"
                                        val photoFile = File(photosDir, fileName)
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            photoFile
                                        )
                                        photoFilePath = "photos/$fileName"
                                        cameraLauncher.launch(uri)
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Camera not available",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Photo")
                            }
                        }
                        FloatingActionButton(onClick = { showFabMenu = !showFabMenu }) {
                            Icon(Icons.Default.Add, contentDescription = "Add note")
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // ---- Tag chips for Tags section ----
                if (uiState.currentSection == HomeSection.TAGS) {
                    TagsSection(
                        viewModel = viewModel
                    )
                } else if (uiState.notes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.StickyNote2,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outlineVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                when {
                                    uiState.currentSection == HomeSection.FAVORITES -> "No favorite notes"
                                    uiState.currentSection == HomeSection.TRASH -> "Trash is empty"
                                    uiState.selectedFolderId != null -> "No notes in this folder"
                                    else -> "No notes yet"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                when {
                                    uiState.currentSection == HomeSection.FAVORITES -> "Star a note to see it here"
                                    uiState.currentSection == HomeSection.TRASH -> "Deleted notes appear here"
                                    uiState.selectedFolderId != null -> "Tap + to add a note here"
                                    else -> "Tap + to create your first note"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else if (uiState.isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.notes, key = { it.id }) { note ->
                            NoteGridItem(
                                note = note,
                                section = uiState.currentSection,
                                onClick = {
                                    if (note.isDeleted) {
                                        viewModel.showRestoreNoteConfirm(note)
                                    } else if (note.content.startsWith("[handwriting/v2]")) {
                                        onNavigateToHandwriting(note.id)
                                    } else {
                                        openNote(note)
                                    }
                                },
                                onLongClick = { viewModel.showNoteContextMenu(note) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.notes, key = { it.id }) { note ->
                            NoteListItem(
                                note = note,
                                section = uiState.currentSection,
                                onClick = {
                                    if (note.isDeleted) {
                                        viewModel.showRestoreNoteConfirm(note)
                                    } else if (note.content.startsWith("[handwriting/v2]")) {
                                        onNavigateToHandwriting(note.id)
                                    } else {
                                        openNote(note)
                                    }
                                },
                                onLongClick = { viewModel.showNoteContextMenu(note) }
                            )
                        }
                    }
                }
            }
        }
    }

    // ---- Note context menu ----
    NoteContextMenu(
        note = uiState.contextMenuNote,
        isTrash = uiState.currentSection == HomeSection.TRASH,
        onDismiss = { viewModel.dismissContextMenu() },
        onTogglePin = { viewModel.onTogglePin(it.id) },
        onToggleStar = { viewModel.onToggleStar(it.id) },
        onMoveToFolder = { viewModel.showMoveNoteDialog(it) },
        onDelete = { viewModel.onDeleteNote(it.id) },
        onRestore = { viewModel.onRestoreNote(it.id) },
        onDeletePermanently = { viewModel.onDeleteNotePermanently(it.id) },
        onShare = { shareNote(context, it) },
        onToggleLock = { viewModel.onToggleLock(it) }
    )

    // ---- Folder context menu ----
    FolderContextMenu(
        folder = uiState.contextMenuFolder,
        onDismiss = { viewModel.dismissContextMenu() },
        onRename = { viewModel.showRenameFolderDialog(it) },
        onDelete = { viewModel.showDeleteFolderConfirm(it) }
    )

    // ---- Dialogs ----
    if (uiState.showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { viewModel.dismissDialog() },
            onConfirm = { name -> viewModel.onCreateFolder(name.trim()) }
        )
    }

    if (uiState.showRenameFolderDialog && uiState.renameFolderTarget != null) {
        RenameFolderDialog(
            currentName = uiState.renameFolderTarget!!.name,
            onDismiss = { viewModel.dismissDialog() },
            onConfirm = { newName ->
                viewModel.onRenameFolder(uiState.renameFolderTarget!!.id, newName.trim())
            }
        )
    }

    if (uiState.showMoveNoteDialog && uiState.moveNoteTarget != null) {
        MoveNoteDialog(
            note = uiState.moveNoteTarget!!,
            folders = uiState.folders,
            onDismiss = { viewModel.dismissDialog() },
            onConfirm = { noteId, folderId ->
                viewModel.onMoveNoteToFolder(noteId, folderId)
            }
        )
    }

    if (uiState.showDeleteFolderConfirm && uiState.deleteFolderTarget != null) {
        DeleteFolderConfirmDialog(
            folderName = uiState.deleteFolderTarget!!.name,
            onDismiss = { viewModel.dismissDialog() },
            onConfirm = { viewModel.onDeleteFolder(uiState.deleteFolderTarget!!.id) }
        )
    }

    if (uiState.showRestoreNoteConfirm && uiState.restoreNoteTarget != null) {
        RestoreNoteConfirmDialog(
            onDismiss = { viewModel.dismissDialog() },
            onConfirm = { viewModel.onRestoreNote(uiState.restoreNoteTarget!!.id) }
        )
    }

    // ---- Settings dialog ----
    if (uiState.showSettingsDialog) {
        SettingsDialog(
            onDismiss = { viewModel.dismissDialog() }
        )
    }
}

// ================================================================
// TAGS SECTION
// ================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        if (uiState.tags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.tags.forEach { tag ->
                    Surface(
                        modifier = Modifier.clickable {
                            viewModel.toggleTagFilter(tag.name)
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = if (uiState.selectedTag == tag.name)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${if (tag.color != null) "●" else "#"} ${tag.name}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${tag.count}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
        if (uiState.tags.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No tags yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// ================================================================
// NOTE LIST ITEM
// ================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteListItem(
    note: Note,
    section: HomeSection,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isDeleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else note.colorHex?.let { Color(android.graphics.Color.parseColor(it)) }
                ?: MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title.ifEmpty { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = if (note.isDeleted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                Row {
                    if (note.isDeleted) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Deleted",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    if (note.isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    if (note.reminderTimestamp != null) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Reminder set",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            if (note.content.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                val isHandwriting = note.content.startsWith("[handwriting/v2]")
                val hasImages = note.content.contains("[img:") || note.content.contains("[drawing:")
                Text(
                    text = when {
                        isHandwriting -> "✏️ Handwriting note"
                        hasImages -> "📷 ${note.content.replace(Regex("""\[(img|drawing):[^\]]+\]"""), "").trim()}"
                        else -> note.content
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (note.isDeleted) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Row {
                Text(
                    text = formatDate(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (note.tags.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = note.tags.joinToString(" · ") { "${it.emoji ?: "#"}${it.name}" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ================================================================
// NOTE GRID ITEM
// ================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteGridItem(
    note: Note,
    section: HomeSection,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isDeleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else note.colorHex?.let { Color(android.graphics.Color.parseColor(it)) }
                ?: MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (note.isPinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = note.title.ifEmpty { "Untitled" },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (note.isDeleted) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            )
            if (note.content.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                val hasImages = note.content.contains("[img:") || note.content.contains("[drawing:")
                val isHandwriting = note.content.startsWith("[handwriting/v2]")
                Text(
                    text = when {
                        isHandwriting -> "✏️ Handwriting note"
                        hasImages -> "📷 ${note.content.replace(Regex("""\[(img|drawing):[^\]]+\]"""), "").trim()}"
                        else -> note.content
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatDate(note.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// ================================================================
// NOTE CONTEXT MENU
// ================================================================

@Composable
private fun NoteContextMenu(
    note: Note?,
    isTrash: Boolean = false,
    onDismiss: () -> Unit,
    onTogglePin: (Note) -> Unit,
    onToggleStar: (Note) -> Unit,
    onMoveToFolder: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onRestore: (Note) -> Unit = {},
    onDeletePermanently: (Note) -> Unit = {},
    onShare: (Note) -> Unit = {},
    onToggleLock: (Note) -> Unit = {}
) {
    if (note != null) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = onDismiss
        ) {
            if (isTrash || note.isDeleted) {
                DropdownMenuItem(
                    text = { Text("Restore") },
                    onClick = { onRestore(note) },
                    leadingIcon = { Icon(Icons.Default.StickyNote2, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete permanently", color = MaterialTheme.colorScheme.error) },
                    onClick = { onDeletePermanently(note) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
            } else {
                DropdownMenuItem(
                    text = { Text(if (note.isPinned) "Unpin" else "Pin") },
                    onClick = { onTogglePin(note) },
                    leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text(if (note.isStarred) "Remove from favorites" else "Add to favorites") },
                    onClick = { onToggleStar(note) },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Move to folder") },
                    onClick = { onMoveToFolder(note) },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Share") },
                    onClick = { onShare(note) },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text(if (note.isLocked) "Unlock" else "Lock") },
                    onClick = { onToggleLock(note) },
                    leadingIcon = {
                        Icon(
                            if (note.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = { onDelete(note) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
        }
    }
}

// ================================================================
// FOLDER CONTEXT MENU
// ================================================================

@Composable
private fun FolderContextMenu(
    folder: Folder?,
    onDismiss: () -> Unit,
    onRename: (Folder) -> Unit,
    onDelete: (Folder) -> Unit
) {
    if (folder != null) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = onDismiss
        ) {
            DropdownMenuItem(
                text = { Text("Rename") },
                onClick = { onRename(folder) },
                leadingIcon = {
                    Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = { onDelete(folder) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}

// ================================================================
// CREATE FOLDER DIALOG
// ================================================================

@Composable
private fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Folder") },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Folder name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(folderName) },
                enabled = folderName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ================================================================
// RENAME FOLDER DIALOG
// ================================================================

@Composable
private fun RenameFolderDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Folder") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Folder name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank()
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ================================================================
// MOVE NOTE TO FOLDER DIALOG
// ================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MoveNoteDialog(
    note: Note,
    folders: List<Pair<Folder, Int>>,
    onDismiss: () -> Unit,
    onConfirm: (noteId: String, folderId: String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move note to folder") },
        text = {
            Column {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onConfirm(note.id, null) }
                        ),
                    shape = RoundedCornerShape(8.dp),
                    color = if (note.folderId == null) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.StickyNote2,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("No folder (root)")
                    }
                }
                Spacer(Modifier.height(8.dp))
                folders.forEach { (folder, count) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onConfirm(note.id, folder.id) }
                            ),
                        shape = RoundedCornerShape(8.dp),
                        color = if (note.folderId == folder.id) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "${folder.iconEmoji ?: "📁"} ${folder.name}",
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ================================================================
// DELETE FOLDER CONFIRMATION DIALOG
// ================================================================

@Composable
private fun DeleteFolderConfirmDialog(
    folderName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Folder") },
        text = {
            Text("Delete \"$folderName\"? Notes in this folder will not be deleted.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ================================================================
// RESTORE NOTE CONFIRMATION DIALOG
// ================================================================

@Composable
private fun RestoreNoteConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore Note") },
        text = { Text("Restore this note to your active notes?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ================================================================
// SETTINGS DIALOG
// ================================================================

@Composable
private fun SettingsDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("MobileNotes") },
        text = {
            Text("Version 1.0\n\nA simple, powerful note-taking app with folders, tags, voice, photo, and handwriting support.")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

// ================================================================
// UTILITY
// ================================================================

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
