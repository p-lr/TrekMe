package com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme

@Composable
fun BatteryOptimWarningDialog(
    consequences: String,
    onDismissRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        title = { Text(text = stringResource(id = R.string.battery_warn_message_gpx_recording_title)) },
        text = {
            BatteryOptimLayout(consequences)
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(id = R.string.ok_dialog))
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
private fun BatteryOptimLayout(consequences: String) {
    Column(
        Modifier.verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.battery_warn_problem_cause),
            style = LocalTextStyle.current.copy(hyphens = Hyphens.Auto)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            consequences,
            style = LocalTextStyle.current.copy(
                hyphens = Hyphens.Auto,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = when {
                android.os.Build.VERSION.SDK_INT >= 35 -> stringResource(R.string.battery_warn_request_action_api35)
                else -> stringResource(R.string.battery_warn_request_action_api34_and_less)
            },
            style = LocalTextStyle.current.copy(
                hyphens = Hyphens.Auto,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.tertiary
            )
        )
    }
}


@Preview(showBackground = true, widthDp = 350)
@Composable
private fun BatteryOptimLayoutPreview() {
    TrekMeTheme {
        BatteryOptimWarningDialog(
            consequences = stringResource(id = R.string.battery_warn_message_gpx_recording),
            onDismissRequest = {},
            onOpenSettings = {}
        )
    }
}