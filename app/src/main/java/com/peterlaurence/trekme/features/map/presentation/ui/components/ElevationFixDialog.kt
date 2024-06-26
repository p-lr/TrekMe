package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme


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
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    label = { Text(text = stringResource(id = R.string.elevation_fix_name)) },
                    onValueChange = {
                        text = it
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(id = R.string.elevation_fix_help),
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Light,
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        onDismiss()
                        onElevationFixUpdate(text.text.toDoubleOrNull()?.toInt() ?: 0)
                    }
                ) {
                    Text(stringResource(id = R.string.save_action))
                }
            }
        }
    )
}

@Preview
@Composable
private fun DialogPreview() {
    TrekMeTheme {
        ElevationFixDialog(elevationFix = 25, onElevationFixUpdate = {}, onDismiss = {})
    }
}