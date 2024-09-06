package com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.peterlaurence.trekme.R

@Composable
fun RecordingRenameDialog(
    id: String,
    name: String,
    onRename: (id: String, newName: String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var currentName by rememberSaveable { mutableStateOf(name) }

    AlertDialog(
        title = { Text(stringResource(id = R.string.track_file_name_change)) },
        text = {
            TextField(value = currentName, onValueChange = { currentName = it })
        },
        confirmButton = {
            TextButton(onClick = { onRename(id, currentName) }) {
                Text(stringResource(id = R.string.ok_dialog))
            }
        },
        dismissButton = {
            TextButton(onClick = { onRename(id, currentName) }) {
                Text(stringResource(id = R.string.cancel_dialog_string))
            }
        },
        onDismissRequest = onDismissRequest
    )
}