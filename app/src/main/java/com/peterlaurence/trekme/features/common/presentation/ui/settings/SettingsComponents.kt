package com.peterlaurence.trekme.features.common.presentation.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import kotlinx.coroutines.launch

@Composable
fun SettingDivider() {
    Divider(thickness = Dp.Hairline)
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
fun ButtonSetting(name: String, subTitle: String? = null, enabled: Boolean, onClick: () -> Unit = {}) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .height(getSettingHeight(hasSubtitle = subTitle != null))
            .clickable(onClick = onClick, enabled = enabled)
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
fun ButtonSettingWithLock(
    name: String,
    subTitle: String? = null,
    enabled: Boolean,
    isLocked: Boolean,
    lockedRationale: String,
    onNavigateToShop: () -> Unit,
    onClick: () -> Unit = {}
) {
    var isShowingLockedRationale by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .height(getSettingHeight(hasSubtitle = subTitle != null))
            .clickable(
                onClick = {
                    if (isLocked) isShowingLockedRationale = true else onClick()
                },
                enabled = enabled
            )
            .padding(start = paddingStart),
        verticalArrangement = Arrangement.Center
    ) {
        if (isLocked) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = name, fontSize = mainFontSize, color = MaterialTheme.colorScheme.onBackground)
                Icon(
                    painter = painterResource(id = R.drawable.ic_lock),
                    modifier = Modifier.padding(start = 8.dp).size(18.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                    contentDescription = null
                )
            }
        } else {
            Text(text = name, fontSize = mainFontSize, color = MaterialTheme.colorScheme.onBackground)
        }

        if (subTitle != null) {
            Text(
                text = subTitle,
                fontSize = subtitleFontSize,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }

    if (isShowingLockedRationale) {
        AlertDialog(
            title = {
                Text(
                    stringResource(id = R.string.map_settings_trekme_extended_title),
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(lockedRationale)
            },
            onDismissRequest = { isShowingLockedRationale = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        isShowingLockedRationale = false
                        onNavigateToShop()
                    }
                ) {
                    Text(stringResource(id = R.string.ok_dialog))
                }
            },
            dismissButton = {
                TextButton(onClick = { isShowingLockedRationale = false }) {
                    Text(text = stringResource(id = R.string.cancel_dialog_string))
                }
            }
        )
    }
}

@Composable
fun <T> ListSetting(
    name: String,
    values: List<Pair<T, String>>,
    selectedIndex: Int,
    subTitle: String? = null,
    onValueSelected: (index: Int, v: T) -> Unit = { _, _ -> }
) {
    var isShowingDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .height(getSettingHeight(hasSubtitle = subTitle != null))
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isShowingDialog = false
                                    onValueSelected(index, pair.first)
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = index == selectedIndex,
                                onClick = {
                                    isShowingDialog = false
                                    onValueSelected(index, pair.first)
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

@Composable
fun EditTextSetting(name: String, value: String, onValueChanged: (String) -> Unit) {
    var isShowingDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .height(getSettingHeight(hasSubtitle = true))
            .clickable { isShowingDialog = true }
            .padding(start = paddingStart),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = name, fontSize = mainFontSize, color = MaterialTheme.colorScheme.onBackground)
        Text(
            text = value,
            fontSize = subtitleFontSize,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }


    if (isShowingDialog) {
        var textFieldValue by remember { mutableStateOf(TextFieldValue(value, selection = TextRange(value.length))) }
        val focusRequester = remember { FocusRequester() }

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
                TextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    modifier = Modifier.focusRequester(focusRequester)
                )
            },
            onDismissRequest = { isShowingDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        isShowingDialog = false
                        onValueChanged(textFieldValue.text)
                    }
                ) {
                    Text(text = stringResource(id = R.string.ok_dialog))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isShowingDialog = false
                        textFieldValue = textFieldValue.copy(text = value)
                    }
                ) {
                    Text(text = stringResource(id = R.string.cancel_dialog_string))
                }
            }
        )

        LaunchedEffect(Unit) {
            runCatching { focusRequester.requestFocus() }
        }
    }
}

private val mainFontSize = 16.sp
private val subtitleFontSize = 14.sp

private val paddingStart = 72.dp
private val settingHeightWithSubtitle = 72.dp
private val settingHeightNoSubtitle = 53.dp

private fun getSettingHeight(hasSubtitle: Boolean) = if (hasSubtitle) settingHeightWithSubtitle else settingHeightNoSubtitle


@Preview
@Composable
private fun SettingPreview() {
    TrekMeTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        var selectedIndex by remember { mutableIntStateOf(0) }
        var subTitle by remember { mutableStateOf("2") }

        var name by remember { mutableStateOf("Mountain View") }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) {
            Column(Modifier.padding(it)) {
                SettingDivider()
                HeaderSetting("Map")
                ButtonSetting(name = "Size", subTitle = "266.2 Mo", enabled = true)
                ListSetting(
                    name = "Number of points",
                    selectedIndex = selectedIndex,
                    values = listOf(
                        "value1" to "2",
                        "value2" to "4",
                        "value3" to "6"
                    ),
                    subTitle = subTitle,
                    onValueSelected = { i, v ->
                        selectedIndex = i
                        subTitle = name
                        scope.launch {
                            snackbarHostState.showSnackbar("selected $v at index $i")
                        }
                    }
                )
                EditTextSetting("Name", name, onValueChanged = { v -> name = v })
            }
        }
    }
}