package com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme

@Composable
fun BatteryOptimSolutionDialog(
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        title = { Text(stringResource(id = R.string.solution_title))},
        text = {
            BatteryOptimLayout()
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.ok_dialog))
            }
        },
        dismissButton = {},
        onDismissRequest = {}, // voluntarily made non-auto dismiss
    )
}

@Composable
private fun BatteryOptimLayout() {
    Column(
        Modifier
            .padding(horizontal = 24.dp)
            .wrapContentHeight()
    ) {
        Text(
            stringResource(id = R.string.battery_warn_solution_msg),
            textAlign = TextAlign.Justify
        )
    }
}

@Preview(showBackground = true, widthDp = 350)
@Composable
private fun BatteryOptimLayoutPreview() {
    TrekMeTheme {
        BatteryOptimSolutionDialog(onDismissRequest = {})
    }
}