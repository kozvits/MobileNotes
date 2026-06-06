package com.mobilenotes.app.presentation.home

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobilenotes.app.domain.model.Folder
import com.mobilenotes.app.domain.model.Note
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToEditor: (String?) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToFolders: () -> Unit = {},
    onNavigateToTags: () -> Unit = {},
    onNavigateToTrash: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onCreatePhotoNote()
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
    // DRAWER
    // ================================================================

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))

                // App header in drawer
                Text(
                    text = "MobileNotes",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp))
                Spacer(Modifier.height(8.dp))

                // All Notes
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("All Notes") },
                    selected = !uiState.showFavorites && uiState.selectedFolderId == null,
                    onClick = {
                        viewModel.showAllNotes()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Favorites
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text("Favorites") },
                    selected = uiState.showFavorites,
                    onClick = {
                        viewModel.toggleShowFavorites()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp))
                Spacer(Modifier.height(8.dp))

                // Settings
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.showSettingsDialog()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // About
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text("About") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.showSettingsDialog()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        // ================================================================
        // MAIN CONTENT
        // ================================================================
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when {
                                uiState.showFavorites -> "Favorites"
                                uiState.selectedFolderId != null ->
                                    uiState.folders.find { it.first.id == uiState.selectedFolderId }?.first?.name
                                        ?: "MobileNotes"
                                else -> "MobileNotes"
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
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
                                    val intent = Intent(
                                        MediaStore.ACTION_IMAGE_CAPTURE
                                    )
                                    cameraLauncher.launch(intent)
                                } catch (_: ActivityNotFoundException) {
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
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // ---- Folder bar (hidden in Favorites mode) ----
                if (!uiState.showFavorites) {
                    FolderBar(
                        folders = uiState.folders,
                        selectedFolderId = uiState.selectedFolderId,
                        onSelectFolder = { viewModel.selectFolder(it) },
                        onCreateFolder = { viewModel.showCreateFolderDialog() },
                        onFolderLongClick = { viewModel.showFolderContextMenu(it) }
                    )
                }

                // ---- Notes content ----
                if (uiState.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading...")
                    }
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
                                    uiState.showFavorites -> "No favorite notes"
                                    uiState.selectedFolderId != null -> "No notes in this folder"
                                    else -> "No notes yet"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                when {
                                    uiState.showFavorites -> "Star a note to see it here"
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
                                onClick = { onNavigateToEditor(note.id) },
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
                                onClick = { onNavigateToEditor(note.id) },
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
        onDismiss = { viewModel.dismissContextMenu() },
        onTogglePin = { viewModel.onTogglePin(it.id) },
        onToggleStar = { viewModel.onToggleStar(it.id) },
        onMoveToFolder = { viewModel.showMoveNoteDialog(it) },
        onDelete = { viewModel.onDeleteNote(it.id) }
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

    // ---- Settings dialog ----
    if (uiState.showSettingsDialog) {
        SettingsDialog(
            onDismiss = { viewModel.dismissDialog() }
        )
    }
}

// ================================================================
// FOLDER BAR
// ================================================================

@Composable
private fun FolderBar(
    folders: List<Pair<Folder, Int>>,
    selectedFolderId: String?,
    onSelectFolder: (String?) -> Unit,
    onCreateFolder: () -> Unit,
    onFolderLongClick: (Folder) -> Unit
) {
    Surface(
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FolderChip(
                    label = "All",
                    icon = if (selectedFolderId == null) Icons.Default.FolderOpen else Icons.Default.Folder,
                    isSelected = selectedFolderId == null,
                    onClick = { onSelectFolder(null) }
                )
            }
            items(folders, key = { it.first.id }) { (folder, noteCount) ->
                FolderChip(
                    label = "${folder.iconEmoji ?: "📁"} ${folder.name}",
                    subLabel = "$noteCount",
                    isSelected = folder.id == selectedFolderId,
                    onClick = { onSelectFolder(folder.id) },
                    onLongClick = { onFolderLongClick(folder) }
                )
            }
            item {
                IconButton(
                    onClick = onCreateFolder,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.CreateNewFolder,
                        contentDescription = "Create folder",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderChip(
    label: String,
    icon: ImageVector? = null,
    subLabel: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            if (subLabel != null) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.labelSmall,
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
            containerColor = note.colorHex?.let { Color(android.graphics.Color.parseColor(it)) }
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
                    modifier = Modifier.weight(1f)
                )
                if (note.isPinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (note.content.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
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
// NOTE GRID ITEM
// ================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteGridItem(
    note: Note,
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
            containerColor = note.colorHex?.let { Color(android.graphics.Color.parseColor(it)) }
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
                overflow = TextOverflow.Ellipsis
            )
            if (note.content.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = note.content,
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
    onDismiss: () -> Unit,
    onTogglePin: (Note) -> Unit,
    onToggleStar: (Note) -> Unit,
    onMoveToFolder: (Note) -> Unit,
    onDelete: (Note) -> Unit
) {
    if (note != null) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = onDismiss
        ) {
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
                onClick = { onDismiss() },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
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
        title = { Text("Delete folder?") },
        text = {
            Text("The folder \"$folderName\" will be deleted. Notes inside will remain but will no longer be assigned to any folder.")
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
            Column {
                Text("Version 1.0")
                Spacer(Modifier.height(8.dp))
                Text(
                    "A simple note-taking app with folders, tags, and markdown support.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

// ================================================================
// DATE FORMATTER
// ================================================================

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
