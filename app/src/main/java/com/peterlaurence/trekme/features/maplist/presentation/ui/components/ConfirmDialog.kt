package com.peterlaurence.trekme.features.maplist.presentation.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.buttons.OutlinedButtonColored
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme

@Composable
fun ConfirmDialog(
    openState: MutableState<Boolean>,
    onConfirmPressed: () -> Unit,
    contentText: String,
    confirmButtonText: String,
    cancelButtonText: String,
    confirmColorBackground: Color = colorResource(id = R.color.colorAccent),
    confirmColorContent: Color = Color.White
) {
    AlertDialog(
        onDismissRequest = { openState.value = false },
        text = {
            Text(contentText, fontSize = 16.sp)
        },
        confirmButton = {
            Button(
                onClick = {
                    openState.value = false
                    onConfirmPressed()
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = confirmColorBackground, contentColor = confirmColorContent)
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            OutlinedButtonColored(
                onClick = {
                    openState.value = false
                },
                color = colorResource(id = R.color.colorDarkGrey),
                text = cancelButtonText
            )
        }
    )
}


@Preview
@Composable
private fun ConfirmDialogPreview() {
    TrekMeTheme {
        val state = remember { mutableStateOf(true) }
        Box {
            ConfirmDialog(
                openState = state,
                onConfirmPressed = { },
                contentText = "Do you really want to delete?",
                confirmButtonText = "Delete",
                cancelButtonText = "Cancel"
            )
        }
    }
}