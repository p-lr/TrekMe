package com.peterlaurence.trekme.features.common.presentation.ui.dialogs

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@Composable
fun ConfirmDialog(
    onConfirmPressed: () -> Unit,
    contentText: String,
    confirmButtonText: String,
    cancelButtonText: String,
    confirmColorBackground: Color? = null,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        text = {
            Text(contentText, fontSize = 16.sp)
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismissRequest()
                    onConfirmPressed()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmColorBackground ?: MaterialTheme.colorScheme.primary,
                )
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismissRequest,
            ) {
                Text(cancelButtonText)
            }
        }
    )
}