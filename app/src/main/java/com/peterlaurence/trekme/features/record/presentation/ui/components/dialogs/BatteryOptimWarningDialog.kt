package com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme

@Composable
fun BatteryOptimWarningDialog(
    onShowSolution: () -> Unit,
    onDismissRequest: () -> Unit
) {

    AlertDialog(
        title = { Text(text = stringResource(id = R.string.warning_title)) },
        text = {
            BatteryOptimLayout()
        },
        confirmButton = {
            TextButton(onClick = onShowSolution) {
                Text(stringResource(id = R.string.battery_warn_solution_btn_txt))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.cancel_dialog_string))
            }
        },
        onDismissRequest = {}, // voluntarily made non-auto dismiss
    )
}

@Composable
private fun BatteryOptimLayout() {
    Surface {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                stringResource(id = R.string.battery_warn_message),
                textAlign = TextAlign.Justify
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 350)
@Composable
private fun BatteryOptimLayoutPreview() {
    TrekMeTheme {
        BatteryOptimWarningDialog(onShowSolution = {}, onDismissRequest = {})
    }
}