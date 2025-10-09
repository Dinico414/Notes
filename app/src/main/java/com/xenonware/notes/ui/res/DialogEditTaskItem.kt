package com.xenonware.notes.ui.res

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf // For managing the list of steps
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.xenonware.notes.R
import com.xenonware.notes.viewmodel.classes.TaskItem
import com.xenonware.notes.viewmodel.classes.TaskStep // Import TaskStep

@Composable
fun DialogEditTaskItem(
    taskItem: TaskItem,
    onDismissRequest: () -> Unit,
    onConfirm: (TaskItem) -> Unit

) {
    var titleState by remember(taskItem.id) { mutableStateOf(taskItem.title) }
    var descriptionState by remember(taskItem.id) { mutableStateOf(taskItem.description ?: "") }

    val currentSteps = remember(taskItem.id) { mutableStateListOf<TaskStep>() }

    XenonDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.edit_task_label),
        properties = DialogProperties(usePlatformDefaultWidth = true),
        contentPadding = PaddingValues(horizontal = 0.dp),
        contentManagesScrolling = true,
    ) {
        TextNoteEditor(
            textState = titleState,
            onTextChange = { titleState = it },
            descriptionState = descriptionState,
            onDescriptionChange = { descriptionState = it },

            onSaveTask = {
                val updatedItem = taskItem.copy(
                    title = titleState.trim(),
                    description = descriptionState.trim().takeIf { it.isNotBlank() },
                )
                onConfirm(updatedItem)
            },
            isSaveEnabled = titleState.isNotBlank(),
            modifier = Modifier
        )
    }
}
