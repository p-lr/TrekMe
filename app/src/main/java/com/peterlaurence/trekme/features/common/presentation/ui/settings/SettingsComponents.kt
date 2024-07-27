package com.peterlaurence.trekme.features.common.presentation.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import kotlinx.coroutines.launch

@Composable
fun SettingDivider() {
    HorizontalDivider(thickness = Dp.Hairline)
}

@Composable
fun HeaderSetting(name: String) {
    Text(
        text = name,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = paddingStart, top = 24.dp, bottom = 12.dp),
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.primary,
        style = TextStyle(
            platformStyle = PlatformTextStyle(
                includeFontPadding = false
            ),
        )
    )
}

@Composable
fun ButtonSetting(
    name: String,
    subTitle: String? = null,
    enabled: Boolean,
    onClick: () -> Unit = {}
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = paddingAround)
            .clickable(onClick = onClick, enabled = enabled)
            .padding(start = paddingStart),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = name,
            fontSize = mainFontSize,
            color = MaterialTheme.colorScheme.onBackground,
            style = nameStyle
        )
        if (subTitle != null) {
            Text(
                text = subTitle,
                fontSize = subtitleFontSize,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                style = subTitleStyle
            )
        }
    }
}

@Composable
fun ButtonSettingWithLock(
    title: @Composable () -> Unit,
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
            .padding(vertical = paddingAround)
            .clickable(
                onClick = {
                    if (isLocked) isShowingLockedRationale = true else onClick()
                },
                enabled = enabled
            )
            .padding(start = paddingStart),
        verticalArrangement = Arrangement.Center
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = mainFontSize,
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false
                )
            )
        ) {
            if (isLocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    title()
                    Icon(
                        painter = painterResource(id = R.drawable.ic_lock),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(18.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                        contentDescription = null
                    )
                }
            } else {
                title()
            }
        }

        if (subTitle != null) {
            Text(
                text = subTitle,
                fontSize = subtitleFontSize,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                style = subTitleStyle
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
fun ToggleSetting(
    name: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onToggle)
            .padding(start = paddingStart, top = 5.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            fontSize = mainFontSize,
            color = MaterialTheme.colorScheme.onBackground,
            style = nameStyle
        )
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
fun LoadingSetting() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = paddingAround)
            .padding(start = paddingStart, end = 16.dp)
    ) {
        LinearProgressIndicator(
            Modifier.fillMaxWidth(),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun <T> ListSetting(
    name: String,
    values: List<Pair<T, String>>,
    selectedValue: T,
    showSubtitle: Boolean = true,
    onValueSelected: (index: Int, v: T) -> Unit = { _, _ -> }
) {
    var isShowingDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = paddingAround)
            .clickable { isShowingDialog = true }
            .padding(start = paddingStart),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = name,
            fontSize = mainFontSize,
            color = MaterialTheme.colorScheme.onBackground,
            style = nameStyle
        )
        if (showSubtitle) {
            Text(
                text = (values.firstOrNull { it.first == selectedValue } ?: values.first()).second,
                fontSize = subtitleFontSize,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                style = subTitleStyle
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
                                selected = pair.first == selectedValue,
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

/**
 * A more customizable version of [ListSetting] which allows for custom subtitles.
 * This function should probably be re-written using ConstraintLayout.
 */
@Composable
fun <T> ListSetting2(
    name: String,
    values: List<Pair<T, ListSettingValue>>,
    selectedValue: T,
    showSubtitle: Boolean = true,
    onValueSelected: (index: Int, v: T) -> Unit = { _, _ -> }
) {
    var isShowingDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = paddingAround)
            .clickable { isShowingDialog = true }
            .padding(start = paddingStart),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = name,
            fontSize = mainFontSize,
            color = MaterialTheme.colorScheme.onBackground,
            style = nameStyle
        )
        if (showSubtitle) {
            Text(
                text = (values.firstOrNull { it.first == selectedValue }
                    ?: values.first()).second.name,
                fontSize = subtitleFontSize,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                style = subTitleStyle
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
                            verticalAlignment = Alignment.Top
                        ) {
                            RadioButton(
                                selected = pair.first == selectedValue,
                                onClick = {
                                    isShowingDialog = false
                                    onValueSelected(index, pair.first)
                                }
                            )
                            if (pair.second.subTitle != null) {
                                Box(Modifier.minimumInteractiveComponentSize()) {
                                    Text(
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .align(Alignment.TopStart),
                                        text = pair.second.name, fontSize = 18.sp
                                    )
                                    Column(Modifier.padding(top = 34.dp)) {
                                        pair.second.subTitle?.invoke()
                                    }
                                }
                            } else {
                                Text(
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                    text = pair.second.name, fontSize = 18.sp
                                )
                            }
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

data class ListSettingValue(val name: String, val subTitle: (@Composable () -> Unit)? = null)

@Composable
fun SliderSetting(
    name: String,
    valueRange: ClosedFloatingPointRange<Float>,
    current: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = paddingAround),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier.padding(start = paddingStart),
            text = name,
            fontSize = mainFontSize,
            color = MaterialTheme.colorScheme.onBackground,
            style = nameStyle
        )
        var value by remember { mutableFloatStateOf(current) }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            Slider(
                modifier = Modifier
                    .padding(start = paddingStart - 7.dp)
                    .width(maxWidth - 140.dp)
                    .align(Alignment.CenterStart),
                valueRange = valueRange,
                value = value,
                onValueChange = { value = it },
                onValueChangeFinished = { onValueChange(value) }
            )
            Text(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .width(51.dp)
                    .align(Alignment.CenterEnd),
                textAlign = TextAlign.Center,
                text = value.toInt().toString()
            )
        }
    }
}

@Composable
fun EditTextSetting(name: String, value: String, onValueChanged: (String) -> Unit) {
    var isShowingDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = paddingAround)
            .clickable { isShowingDialog = true }
            .padding(start = paddingStart),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = name,
            fontSize = mainFontSize,
            color = MaterialTheme.colorScheme.onBackground,
            style = nameStyle
        )
        Text(
            text = value,
            fontSize = subtitleFontSize,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            style = subTitleStyle
        )
    }


    if (isShowingDialog) {
        var textFieldValue by remember {
            mutableStateOf(
                TextFieldValue(
                    value,
                    selection = TextRange(value.length)
                )
            )
        }
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
private val paddingAround = 16.dp
private val nameStyle = TextStyle(
    platformStyle = PlatformTextStyle(
        includeFontPadding = false
    ),
    lineHeight = 21.sp,
    lineHeightStyle = LineHeightStyle(
        trim = LineHeightStyle.Trim.FirstLineTop,
        alignment = LineHeightStyle.Alignment.Top
    )
)
private val subTitleStyle = TextStyle(
    platformStyle = PlatformTextStyle(
        includeFontPadding = false
    ),
)


@Preview
@Composable
private fun SettingPreview() {
    TrekMeTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        var selectedValue by remember { mutableStateOf("value1") }
        var name by remember { mutableStateOf("Mountain View") }
        var toggled by remember { mutableStateOf(false) }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) {
            Column(Modifier.padding(it)) {
                SettingDivider()
                HeaderSetting("Map")
                ButtonSetting(name = "Size", subTitle = "266.2 Mo", enabled = true)
                ListSetting(
                    name = "Number of points",
                    selectedValue = selectedValue,
                    values = listOf(
                        "value1" to "2",
                        "value2" to "4",
                        "value3" to "6"
                    ),
                    showSubtitle = true,
                    onValueSelected = { i, v ->
                        selectedValue = v
                        scope.launch {
                            snackbarHostState.showSnackbar("selected $v at index $i")
                        }
                    }
                )
                EditTextSetting("Name", name, onValueChanged = { v -> name = v })
                ToggleSetting(
                    name = "An option",
                    checked = toggled,
                    onToggle = { toggled = !toggled })
                SliderSetting(name = "A slider option", valueRange = 0f..100f, current = 40f) {}
            }
        }
    }
}