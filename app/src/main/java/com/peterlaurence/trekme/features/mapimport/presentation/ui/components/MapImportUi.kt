package com.peterlaurence.trekme.features.mapimport.presentation.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.mapimport.presentation.viewmodel.MapImportViewModel

@Composable
fun MapImportUiStateful(
    viewModel: MapImportViewModel,
    onImportClicked: () -> Unit = {}
) {
    val isImporting by viewModel.isImporting.collectAsState()

    Surface {
        MapImportUi(isImporting = isImporting, onImportClicked = onImportClicked)
    }
}

@Composable
private fun MapImportUi(isImporting: Boolean, onImportClicked: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.import_30dp),
            contentDescription = stringResource(
                id = R.string.recording_import_desc
            ),
            modifier = Modifier
                .size(150.dp)
                .padding(24.dp)
                .alpha(0.5f)
        )

        Text(
            text = stringResource(id = R.string.import_desc),
            modifier = Modifier.padding(horizontal = 48.dp),
            textAlign = TextAlign.Justify
        )

        Button(
            onClick = onImportClicked,
            enabled = !isImporting,
            modifier = Modifier.padding(vertical = 24.dp)
        ) {
            Text(stringResource(id = R.string.import_folder_select_btn))
        }

        if (isImporting) {
            LinearProgressIndicator()
        }
    }
}

@Preview(showBackground = true, widthDp = 350, heightDp = 400)
@Preview(showBackground = true, widthDp = 350, heightDp = 400, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun MapImportUiPreview() {
    TrekMeTheme {
        MapImportUi(isImporting = false)
    }
}