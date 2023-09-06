package com.peterlaurence.trekme.features.common.presentation.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import kotlinx.coroutines.launch

@Composable
fun SettingDivider() {
    Divider()
}

@Composable
fun HeaderSetting(name: String) {
    Text(
        text = name,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = paddingStart, top = 24.dp, bottom = 8.dp),
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun ButtonSetting(name: String, subTitle: String? = null, onClick: () -> Unit = {}) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .height(settingHeight)
            .clickable(onClick = onClick)
            .padding(start = paddingStart),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = name, fontSize = mainFontSize, color = MaterialTheme.colorScheme.onBackground)
        if (subTitle != null) {
            Text(
                text = subTitle,
                fontSize = subtitleFontSize,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun <T> ListSetting(
    name: String,
    values: List<Pair<T, String>>,
    selectedIndex: Int,
    subTitle: String? = null,
    onValueSelected: (index: Int, v: T, name: String) -> Unit = { _, _, _ -> }
) {
    var isShowingDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .height(settingHeight)
            .clickable { isShowingDialog = true }
            .padding(start = paddingStart),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = name, fontSize = mainFontSize, color = MaterialTheme.colorScheme.onBackground)
        if (subTitle != null) {
            Text(
                text = subTitle,
                fontSize = subtitleFontSize,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }

    if (isShowingDialog) {
        AlertDialog(
            title = {
                Text(
                    text = name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column {
                    values.forEachIndexed { index, pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                isShowingDialog = false
                                onValueSelected(index, pair.first, pair.second)
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = index == selectedIndex,
                                onClick = {
                                    isShowingDialog = false
                                    onValueSelected(index, pair.first, pair.second)
                                }
                            )
                            Text(text = pair.second, fontSize = 18.sp)
                        }
                    }
                }
            },
            onDismissRequest = { isShowingDialog = false },
            confirmButton = {
                TextButton(onClick = { isShowingDialog = false }) {
                    Text(text = stringResource(id = R.string.cancel_dialog_string))
                }
            }
        )
    }
}

private val mainFontSize = 16.sp
private val subtitleFontSize = 14.sp

private val paddingStart = 72.dp
private val settingHeight = 72.dp


@Preview
@Composable
private fun SettingPreview() {
    TrekMeTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        var selectedIndex by remember { mutableIntStateOf(0) }
        var subTitle by remember { mutableStateOf("2") }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) {
            Column(Modifier.padding(it)) {
                SettingDivider()
                HeaderSetting("Map")
                ButtonSetting(name = "Size", subTitle = "266.2 Mo")
                ListSetting(
                    name = "Number of points",
                    selectedIndex = selectedIndex,
                    values = listOf(
                        "value1" to "2",
                        "value2" to "4",
                        "value3" to "6"
                    ),
                    subTitle = subTitle,
                    onValueSelected = { i, v, name ->
                        selectedIndex = i
                        subTitle = name
                        scope.launch {
                            snackbarHostState.showSnackbar("selected $v at index $i")
                        }
                    }
                )
            }
        }
    }
}