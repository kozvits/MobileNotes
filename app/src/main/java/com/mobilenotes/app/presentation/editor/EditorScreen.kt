package com.mobilenotes.app.presentation.editor

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Looks3
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.LooksTwo
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobilenotes.app.presentation.components.DrawingDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

enum class MarkdownCommand {
    H1, H2, H3, BOLD, ITALIC, UNDERLINE, LIST, QUOTE, DIVIDER, DRAW, IMAGE
}

/**
 * Apply a markdown formatting command to the current selection.
 * When no text is selected, markers are inserted at the cursor and the
 * cursor is placed between them (so typing continues inside).
 */
private fun applyMarkdownCommand(
    value: TextFieldValue,
    command: MarkdownCommand
): TextFieldValue {
    val text = value.text
    val sel = value.selection
    val start = sel.start.coerceIn(0, text.length)
    val end = sel.end.coerceIn(0, text.length)
    val selected = text.substring(start, end)

    fun wrap(prefix: String, suffix: String = prefix, blockPrefix: String = ""): TextFieldValue {
        val newText = buildString {
            append(text.substring(0, start))
            append(prefix)
            append(selected)
            append(suffix)
            append(text.substring(end))
        }
        val caret = start + prefix.length + selected.length + suffix.length
        return TextFieldValue(
            text = newText,
            selection = TextRange(if (selected.isEmpty()) start + prefix.length else caret)
        )
    }

    fun linePrefix(prefix: String): TextFieldValue {
        // Find line start of selection
        val lineStart = text.lastIndexOf('\n', start - 1) + 1
        val newText = buildString {
            append(text.substring(0, lineStart))
            append(prefix)
            append(text.substring(lineStart))
        }
        val caret = end + prefix.length
        return TextFieldValue(text = newText, selection = TextRange(caret))
    }

    return when (command) {
        MarkdownCommand.H1 -> linePrefix("# ")
        MarkdownCommand.H2 -> linePrefix("## ")
        MarkdownCommand.H3 -> linePrefix("### ")
        MarkdownCommand.BOLD -> wrap("**")
        MarkdownCommand.ITALIC -> wrap("*")
        MarkdownCommand.UNDERLINE -> wrap("__")
        MarkdownCommand.LIST -> linePrefix("- ")
        MarkdownCommand.QUOTE -> linePrefix("> ")
        MarkdownCommand.DIVIDER -> wrap("\n\n---\n\n")
        MarkdownCommand.DRAW -> value // handled by caller (opens dialog)
        MarkdownCommand.IMAGE -> value // handled by caller (opens gallery picker)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    noteId: String? = null,
    onNavigateBack: () -> Unit = {},
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(uiState.content))
    }
    var titleValue by remember { mutableStateOf(TextFieldValue(uiState.title)) }

    // Sync from loaded note (only when not actively editing to avoid cursor jumps)
    LaunchedEffect(uiState.noteId, uiState.content) {
        if (textFieldValue.text != uiState.content && !uiState.isNew) {
            textFieldValue = TextFieldValue(uiState.content)
        }
    }
    LaunchedEffect(uiState.title) {
        if (titleValue.text != uiState.title) titleValue = TextFieldValue(uiState.title)
    }

    // Full-screen image viewer
    var fullScreenImagePath by remember { mutableStateOf<String?>(null) }
    var showDrawingDialog by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    // Intermediate calendar used to assemble the reminder timestamp from date + time.
    var reminderCalendar by remember { mutableStateOf(java.util.Calendar.getInstance()) }

    // Pick an image from the gallery and embed it as [img:path]
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri ?: return@rememberLauncherForActivityResult
            val savedName = copyUriToInternalStorage(context, uri) ?: return@rememberLauncherForActivityResult
            val marker = "[img:$savedName]"
            val next = textFieldValue.copy(text = textFieldValue.text + "\n\n$marker")
            textFieldValue = next
            viewModel.onContentChanged(next.text)
            viewModel.scheduleAutoSave()
        }
    )

    val imagePaths = remember(textFieldValue.text) {
        val regex = Regex("""\[(img|drawing):([^\]]+)\]""")
        regex.findAll(textFieldValue.text).map { it.groupValues[2] }.toList()
    }

    fun onFormat(command: MarkdownCommand) {
        when (command) {
            MarkdownCommand.DRAW -> showDrawingDialog = true
            MarkdownCommand.IMAGE -> imagePicker.launch("image/*")
            else -> {
                val next = applyMarkdownCommand(textFieldValue, command)
                textFieldValue = next
                viewModel.onContentChanged(next.text)
                viewModel.scheduleAutoSave()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isNew) "New Note" else "Edit Note",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveOnExit()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        Text(
                            "Saving...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(onClick = { viewModel.save() }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FormattingToolbar(onFormat = ::onFormat)

            Divider()

            TagBar(
                tags = uiState.tags,
                onAddClick = { showTagPicker = true },
                onRemove = { tag -> viewModel.onTagsChanged(uiState.tags - tag) }
            )

            Divider()

            // Reminder row
            ReminderRow(
                reminderTimestamp = uiState.reminderTimestamp,
                onPick = { showDatePicker = true },
                onClear = { viewModel.onReminderChanged(null) }
            )

            Divider()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                BasicTextField(
                    value = titleValue,
                    onValueChange = {
                        titleValue = it
                        viewModel.onTitleChanged(it.text)
                    },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (titleValue.text.isEmpty()) {
                            Text(
                                "Title",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                BasicTextField(
                    value = textFieldValue,
                    onValueChange = {
                        textFieldValue = it
                        viewModel.onContentChanged(it.text)
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 28.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                "Start writing...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (imagePaths.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    imagePaths.forEach { path ->
                        val imageFile = File(context.filesDir, path)
                        if (imageFile.exists()) {
                            val bitmap = remember(imageFile) {
                                BitmapFactory.decodeFile(imageFile.absolutePath)
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = if (path.startsWith("drawing")) "Drawing" else "Photo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { fullScreenImagePath = path },
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(100.dp))
            }
        }
    }

    if (fullScreenImagePath != null) {
        FullScreenImageDialog(
            imagePath = fullScreenImagePath!!,
            onDismiss = { fullScreenImagePath = null }
        )
    }

    if (showDrawingDialog) {
        DrawingDialog(
            onDismiss = { showDrawingDialog = false },
            onSave = { filePath ->
                val marker = "[drawing:$filePath]"
                val next = textFieldValue.copy(
                    text = textFieldValue.text + "\n\n$marker"
                )
                textFieldValue = next
                viewModel.onContentChanged(next.text)
                viewModel.scheduleAutoSave()
            }
        )
    }

    if (showTagPicker) {
        TagPickerDialog(
            currentTags = uiState.tags,
            onTagsChanged = { viewModel.onTagsChanged(it) },
            onDismiss = { showTagPicker = false }
        )
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.reminderTimestamp ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = dateState.selectedDateMillis
                    if (millis != null) {
                        val cal = java.util.Calendar.getInstance().apply {
                            timeInMillis = reminderCalendar.timeInMillis
                            set(java.util.Calendar.YEAR, dateState.selectedDateMillis!!.let {
                                val c = java.util.Calendar.getInstance().apply { timeInMillis = it }
                                c.get(java.util.Calendar.YEAR)
                            })
                            set(java.util.Calendar.MONTH, dateState.selectedDateMillis!!.let {
                                val c = java.util.Calendar.getInstance().apply { timeInMillis = it }
                                c.get(java.util.Calendar.MONTH)
                            })
                            set(java.util.Calendar.DAY_OF_MONTH, dateState.selectedDateMillis!!.let {
                                val c = java.util.Calendar.getInstance().apply { timeInMillis = it }
                                c.get(java.util.Calendar.DAY_OF_MONTH)
                            })
                        }
                        reminderCalendar = cal
                    }
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = reminderCalendar.get(java.util.Calendar.HOUR_OF_DAY),
            initialMinute = reminderCalendar.get(java.util.Calendar.MINUTE)
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = reminderCalendar
                    cal.set(java.util.Calendar.HOUR_OF_DAY, timeState.hour)
                    cal.set(java.util.Calendar.MINUTE, timeState.minute)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    viewModel.onReminderChanged(cal.timeInMillis)
                    showTimePicker = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timeState) }
        )
    }
}

@Composable
private fun FullScreenImageDialog(
    imagePath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.95f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            val imageFile = File(context.filesDir, imagePath)
            if (imageFile.exists()) {
                val bitmap = remember(imageFile) {
                    BitmapFactory.decodeFile(imageFile.absolutePath)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Full size",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        RoundedCornerShape(50)
                    )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun FormattingToolbar(
    onFormat: (MarkdownCommand) -> Unit
) {
    val items = listOf(
        Icons.Default.LooksOne to "H1" to MarkdownCommand.H1,
        Icons.Default.LooksTwo to "H2" to MarkdownCommand.H2,
        Icons.Default.Looks3 to "H3" to MarkdownCommand.H3,
        Icons.Default.FormatBold to "Bold" to MarkdownCommand.BOLD,
        Icons.Default.FormatItalic to "Italic" to MarkdownCommand.ITALIC,
        Icons.Default.FormatListBulleted to "List" to MarkdownCommand.LIST,
        Icons.Default.FormatQuote to "Quote" to MarkdownCommand.QUOTE,
        Icons.Default.HorizontalRule to "Divider" to MarkdownCommand.DIVIDER,
        Icons.Default.Image to "Image" to MarkdownCommand.IMAGE,
        Icons.Default.Create to "Draw" to MarkdownCommand.DRAW
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (iconLabel, cmd) ->
            val (icon, label) = iconLabel
            FormatButton(icon, label) { onFormat(cmd) }
        }
    }
}

@Composable
private fun FormatButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Copy an image [Uri] picked from the gallery into the app's internal storage
 * and return the relative file name (stored under [Context.filesDir]).
 * Returns null if the copy fails.
 */
private fun copyUriToInternalStorage(context: android.content.Context, uri: Uri): String? {
    return try {
        val ext = when (context.contentResolver.getType(uri)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val name = "img_${System.currentTimeMillis()}.$ext"
        context.contentResolver.openInputStream(uri)?.use { input ->
            context.openFileOutput(name, android.content.Context.MODE_PRIVATE).use { out ->
                input.copyTo(out)
            }
        }
        name
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
private fun ReminderRow(
    reminderTimestamp: Long?,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    val formatter = remember {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (reminderTimestamp != null) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
            contentDescription = "Reminder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = if (reminderTimestamp != null) {
                "Reminder: ${formatter.format(java.util.Date(reminderTimestamp))}"
            } else {
                "No reminder"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (reminderTimestamp != null) {
            TextButton(onClick = onClear) { Text("Clear") }
        }
        TextButton(onClick = onPick) { Text(if (reminderTimestamp != null) "Change" else "Set") }
    }
}
