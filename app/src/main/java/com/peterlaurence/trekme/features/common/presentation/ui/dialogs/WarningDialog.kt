package com.peterlaurence.trekme.features.common.presentation.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R

@Composable
fun WarningDialog(
    title: String,
    contentText: String,
    confirmButtonText: String = stringResource(id = R.string.ok_dialog),
    onConfirmPressed: () -> Unit = {},
    confirmColorBackground: Color? = null,
    dismissButtonText: String? = null,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        title = {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        },
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
        dismissButton = dismissButtonText?.let {
            {
                Button(
                    onClick = {
                        onDismissRequest()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = confirmColorBackground
                            ?: MaterialTheme.colorScheme.primary,
                    )
                ) {
                    Text(it)
                }
            }
        },
        onDismissRequest = onDismissRequest,
    )
}