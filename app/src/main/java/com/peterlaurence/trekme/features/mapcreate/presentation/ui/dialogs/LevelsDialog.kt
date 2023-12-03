package com.peterlaurence.trekme.features.mapcreate.presentation.ui.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.wmts.domain.tools.getNumberOfTiles
import com.peterlaurence.trekme.core.wmts.domain.tools.toSizeInMo
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.model.Point
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.model.toDomain
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@Composable
fun LevelsDialogStateful(
    minLevel: Int,
    maxLevel: Int,
    startMinLevel: Int = 12,
    startMaxLevel: Int = 16,
    p1: Point,
    p2: Point,
    onDownloadClicked: (minLevel: Int, maxLevel: Int) -> Unit = { _, _ -> },
    onDismiss: () -> Unit = {}
) {
    val range = maxLevel - minLevel
    val step = 1f / range

    var min by rememberSaveable {
        mutableFloatStateOf(normalizedValue(startMinLevel, minLevel, range))
    }
    var max by rememberSaveable {
        mutableFloatStateOf(normalizedValue(startMaxLevel, minLevel, range))
    }

    val minDenormalized = remember(min) {
        deNormalizedValue(min, minLevel, range)
    }

    val maxDenormalized = remember(max) {
        deNormalizedValue(max, minLevel, range)
    }

    val tilesNumber = remember(minDenormalized, maxDenormalized) {
        getNumberOfTiles(
            levelMin = minDenormalized,
            levelMax = maxDenormalized,
            point1 = p1.toDomain(),
            point2 = p2.toDomain()
        )
    }
    val mapSizeInMo = remember(tilesNumber) {
        tilesNumber.toSizeInMo()
    }

    AlertDialog(
        title = { Text(stringResource(id = R.string.wmts_settings_dialog)) },
        text = {
            LevelsDialog(
                min = min,
                max = max,
                minDenormalized = minDenormalized,
                maxDenormalized = maxDenormalized,
                range = range,
                onMinChange = {
                    min = it
                    max = max.coerceAtLeast(min)
                },
                onMaxChange = {
                    max = it
                    min = min.coerceAtMost(max)
                },
                onMinDecrement = { min = (min - step).coerceAtLeast(0f) },
                onMinIncrement = {
                    min = (min + step).coerceAtMost(1f)
                    max = max.coerceAtLeast(min)
                },
                onMaxDecrement = {
                    max = (max - step).coerceAtLeast(0f)
                    min = min.coerceAtMost(max)
                },
                onMaxIncrement = { max = (max + step).coerceAtMost(1f) },
                mapSizeInMo = mapSizeInMo
            )
        },
        confirmButton = {
            TextButton(onClick = { onDownloadClicked(minDenormalized, maxDenormalized) }) {
                Text(stringResource(id = R.string.download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel_dialog_string))
            }
        },
        onDismissRequest = onDismiss
    )
}

@Composable
private fun LevelsDialog(
    min: Float,
    max: Float,
    minDenormalized: Int,
    maxDenormalized: Int,
    range: Int,
    onMinChange: (Float) -> Unit,
    onMaxChange: (Float) -> Unit,
    onMinDecrement: () -> Unit,
    onMinIncrement: () -> Unit,
    onMaxDecrement: () -> Unit,
    onMaxIncrement: () -> Unit,
    mapSizeInMo: Long
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text(stringResource(id = R.string.min_zoom), fontWeight = FontWeight.Medium)
        SlideAndControls(
            value = min,
            valueDeNormalized = minDenormalized,
            range = range,
            onValueChange = onMinChange,
            onDecrement = onMinDecrement,
            onIncrement = onMinIncrement
        )

        Text(
            stringResource(id = R.string.max_zoom),
            Modifier.padding(top = 16.dp),
            fontWeight = FontWeight.Medium
        )
        SlideAndControls(
            value = max,
            valueDeNormalized = maxDenormalized,
            range = range,
            onValueChange = onMaxChange,
            onDecrement = onMaxDecrement,
            onIncrement = onMaxIncrement
        )
        MapSizeLine(Modifier.padding(top = 16.dp), mapSizeInMo = mapSizeInMo)
    }
}

@Composable
private fun SlideAndControls(
    value: Float,
    valueDeNormalized: Int,
    range: Int,
    onValueChange: (Float) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDecrement) {
            Icon(
                painter = painterResource(id = R.drawable.minus_box),
                contentDescription = stringResource(id = R.string.wmts_level_min_decrement),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            steps = range - 1,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onIncrement) {
            Icon(
                painter = painterResource(id = R.drawable.plus_box),
                contentDescription = stringResource(id = R.string.wmts_level_min_increment),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = valueDeNormalized.toString(),
            modifier = Modifier
                .padding(start = 12.dp),
            fontWeight = FontWeight.Medium,
        )
    }
}


private fun normalizedValue(level: Int, minLevel: Int, range: Int): Float {
    return ((level - minLevel).toFloat() / range).coerceIn(0f, 1f)
}

private fun deNormalizedValue(level: Float, minLevel: Int, range: Int): Int {
    return minLevel + (range * level).toInt()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapSizeLine(modifier: Modifier = Modifier, mapSizeInMo: Long) {
    val locale = LocalConfiguration.current.locales.get(0) ?: Locale.ENGLISH
    val numberFormat = remember {
        NumberFormat.getNumberInstance(locale)
    }

    Row(
        modifier
            .fillMaxWidth()
            .height(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(id = R.string.map_size), fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "${numberFormat.format(mapSizeInMo)} Mo",
            fontWeight = FontWeight.Medium
        )
        if (mapSizeInMo > 200) {
            Spacer(modifier = Modifier.width(16.dp))
            val tooltipState = remember { RichTooltipState() }
            val scope = rememberCoroutineScope()
            RichTooltipBox(
                title = { Text(stringResource(id = R.string.map_too_big)) },
                action = {
                    TextButton(
                        onClick = { scope.launch { tooltipState.dismiss() } }
                    ) { Text("Ok") }
                },
                text = { Text(stringResource(id = R.string.map_too_big_explanation)) },
                tooltipState = tooltipState
            ) {
                Image(
                    painter = painterResource(id = R.drawable.warning),
                    modifier = Modifier
                        .tooltipAnchor()
                        .clickable {
                            scope.launch {
                                tooltipState.show()
                            }
                        },
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.tertiary),
                    contentDescription = null
                )
            }

        } else Modifier.width(35.dp)
    }
}

@Preview(showBackground = true, widthDp = 350, heightDp = 400)
@Composable
fun LevelsDialogPreview() {
    TrekMeTheme {
        LevelsDialogStateful(
            minLevel = 1,
            maxLevel = 18,
            p1 = Point(0.0, 0.0),
            p2 = Point(100000.0, 100000.0),
            onDownloadClicked = { min, max ->
                println("Download using min=$min max=$max")
            }
        )
    }
}