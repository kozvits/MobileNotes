package com.mobilenotes.app.presentation.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobilenotes.app.domain.model.Tag

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagBar(
    tags: List<Tag>,
    onAddClick: () -> Unit,
    onRemove: (Tag) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Label,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        if (tags.isEmpty()) {
            Text(
                text = "No tags",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 8.dp)
            )
        } else {
            FlowRow(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tags.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = tag.color?.let { Color(android.graphics.Color.parseColor(it)) }
                            ?: MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.clickable { onRemove(tag) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${tag.emoji ?: "#"} ${tag.name}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove tag",
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(start = 4.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
        IconButton(onClick = onAddClick, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add tag",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TagPickerDialog(
    currentTags: List<Tag>,
    onTagsChanged: (List<Tag>) -> Unit,
    onDismiss: () -> Unit,
    viewModel: TagPickerViewModel = hiltViewModel()
) {
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    var newTagText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val currentIds = currentTags.map { it.id }.toSet()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tags") },
        text = {
            Column {
                // Existing tags (toggle)
                FlowRowOrColumn(
                    tags = allTags,
                    currentIds = currentIds,
                    onToggle = { tag ->
                        val updated = if (tag.id in currentIds) {
                            currentTags - tag
                        } else {
                            currentTags + tag
                        }
                        onTagsChanged(updated)
                    }
                )

                if (allTags.isEmpty()) {
                    Text(
                        "No tags yet — create one below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))

                // Create new tag
                OutlinedTextField(
                    value = newTagText,
                    onValueChange = { newTagText = it },
                    label = { Text("New tag") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = newTagText.isNotBlank(),
                onClick = {
                    scope.launch {
                        val tag = viewModel.ensureTag(newTagText)
                        onTagsChanged(currentTags + tag)
                        newTagText = ""
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowOrColumn(
    tags: List<Tag>,
    currentIds: Set<String>,
    onToggle: (Tag) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tags.forEach { tag ->
            val selected = tag.id in currentIds
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (selected)
                    MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onToggle(tag) }
            ) {
                Text(
                    text = "${tag.emoji ?: "#"} ${tag.name}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
