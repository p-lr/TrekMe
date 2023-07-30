package com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme

/**
 * This rationale explains why we ask for background location permission when the user starts a GPX
 * recording.
 * The user can opt-in for "never show this again" (see [onIgnore]).
 */
@Composable
fun LocationRationale(
    onConfirm: () -> Unit,
    onIgnore: () -> Unit,
) {
    AlertDialog(
        title = { Text(text = stringResource(id = R.string.location_info_title)) },
        text = { Text(stringResource(id = R.string.background_location_disclaimer)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(id = R.string.ok_dialog))
            }
        },
        dismissButton = {
            TextButton(onClick = onIgnore) {
                Text(text = stringResource(id = R.string.no_authorize_dialog))
            }
        },
        onDismissRequest = {}  // on purpose
    )
}

@Preview(locale="en")
@Preview(locale="fr")
@Composable
private fun LocationRationalePreview() {
    TrekMeTheme {
        LocationRationale({}, {})
    }
}