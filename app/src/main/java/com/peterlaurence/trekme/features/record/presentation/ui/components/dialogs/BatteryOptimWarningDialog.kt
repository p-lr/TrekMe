package com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme

@Composable
fun BatteryOptimWarningDialog(
    text: String,
    onShowSolution: () -> Unit,
    onDismissRequest: () -> Unit
) {

    AlertDialog(
        title = { Text(text = stringResource(id = R.string.warning_title)) },
        text = {
            BatteryOptimLayout(text)
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
private fun BatteryOptimLayout(text: String) {
    Surface {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(text, textAlign = TextAlign.Justify, style = LocalTextStyle.current.copy(hyphens = Hyphens.Auto))
        }
    }
}

@Preview(showBackground = true, widthDp = 350)
@Composable
private fun BatteryOptimLayoutPreview() {
    TrekMeTheme {
        BatteryOptimWarningDialog("Some warning", onShowSolution = {}, onDismissRequest = {})
    }
}