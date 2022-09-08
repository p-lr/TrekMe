package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.accentColor

@Composable
fun ElevationFixDialog(
    elevationFix: Int,
    onElevationFixUpdate: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(TextFieldValue(elevationFix.toString())) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.elevation_fix_title))
        },
        buttons = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor()),
                    onClick = {
                        onDismiss()
                        onElevationFixUpdate(text.text.toDoubleOrNull()?.toInt() ?: 0)
                    }
                ) {
                    Text(stringResource(id = R.string.save_action))
                }
            }
        },
        text = {
            OutlinedTextField(
                value = text,
                label = { Text(text = stringResource(id = R.string.elevation_fix_name)) },
                onValueChange = {
                    text = it
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    )
}