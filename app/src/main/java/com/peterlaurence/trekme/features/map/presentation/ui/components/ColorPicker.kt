package com.peterlaurence.trekme.features.map.presentation.ui.components

import android.content.res.Configuration
import android.os.Parcelable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import kotlinx.parcelize.Parcelize

@Composable
fun ColorPicker(
    initColor: Long = 0L,
    onColorPicked: (Long) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val initValues = remember(initColor) {
        findColorInPalettes(initColor)
    }
    var paletteVariant by remember { mutableStateOf(initValues?.first ?: PaletteVariant.NORMAL) }
    val palette by remember {
        derivedStateOf {
            when (paletteVariant) {
                PaletteVariant.NORMAL -> normalPalette
                PaletteVariant.LIGHT -> lightPalette
                PaletteVariant.DARK -> darkPalette
            }
        }
    }
    var activeIndex by remember { mutableIntStateOf(initValues?.second ?: 0) }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                stringResource(id = R.string.color_picker_title)
            )
        },
        text = {
            Column(
                Modifier
                    .verticalScroll(scrollState)
            ) {
                val strokeColor = MaterialTheme.colorScheme.onSurface
                Spacer(modifier = Modifier.height(8.dp))
                ColorRow(
                    activeIndex,
                    palette,
                    offset = 0,
                    strokeColor = strokeColor,
                    onClick = { activeIndex = it }
                )

                Spacer(modifier = Modifier.height(16.dp))
                ColorRow(
                    activeIndex,
                    palette,
                    offset = 3,
                    strokeColor = strokeColor,
                    onClick = { activeIndex = it }
                )

                Spacer(modifier = Modifier.height(16.dp))
                ColorRow(
                    activeIndex,
                    palette,
                    offset = 6,
                    strokeColor,
                    onClick = { activeIndex = it }
                )

                Row(Modifier.padding(start = 16.dp, top = 16.dp)) {
                    Text(
                        stringResource(id = R.string.color_variant_label),
                        modifier = Modifier.padding(top = 13.dp),
                        fontWeight = FontWeight.Medium,
                    )

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                paletteVariant = PaletteVariant.NORMAL
                            }
                        ) {
                            RadioButton(
                                selected = paletteVariant == PaletteVariant.NORMAL,
                                onClick = { paletteVariant = PaletteVariant.NORMAL }
                            )
                            Text(text = stringResource(id = R.string.color_variant_normal))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                paletteVariant = PaletteVariant.LIGHT
                            }
                        ) {
                            RadioButton(
                                selected = paletteVariant == PaletteVariant.LIGHT,
                                onClick = { paletteVariant = PaletteVariant.LIGHT }
                            )
                            Text(text = stringResource(id = R.string.color_variant_light))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                paletteVariant = PaletteVariant.DARK
                            }
                        ) {
                            RadioButton(
                                selected = paletteVariant == PaletteVariant.DARK,
                                onClick = { paletteVariant = PaletteVariant.DARK }
                            )
                            Text(text = stringResource(id = R.string.color_variant_dark))
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(
                    text = stringResource(id = R.string.cancel_dialog_string),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorPicked(palette[activeIndex]) }) {
                Text(
                    text = stringResource(id = R.string.apply_color_btn),
                )
            }
        }
    )
}

@Composable
private fun ColorRow(
    activeIndex: Int,
    palette: List<Long>,
    offset: Int,
    strokeColor: Color,
    onClick: (index: Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            Surface(
                onClick = { onClick(offset + index) },
                shape = CircleShape,
                color = Color(palette[offset + index]),
                modifier = Modifier
                    .padding(horizontal = if (offset + index == activeIndex) 0.dp else 3.dp)
                    .size(if (offset + index == activeIndex) 36.dp else 30.dp)
                    .drawBehind {
                        if (offset + index == activeIndex) {
                            drawCircle(
                                strokeColor,
                                19.dp.toPx(),
                                style = Stroke(3.dp.toPx())
                            )
                        }
                    }
            ) {}
        }
    }
}

private fun findColorInPalettes(color: Long): Pair<PaletteVariant, Int>? {
    PaletteVariant.entries.forEach { variant ->
        val palette = when (variant) {
            PaletteVariant.NORMAL -> normalPalette
            PaletteVariant.LIGHT -> lightPalette
            PaletteVariant.DARK -> darkPalette
        }
        val index = palette.indexOfFirst { it == color }
        if (index != -1) {
            return Pair(variant, index)
        }
    }
    return null
}

private val normalPalette = listOf(
    0xfff44336, 0xff9c27b0, 0xff2196f3, 0xff4caf50, 0xff755548, 0xff607d8b,
    0xffFF9800, 0xffffeb3b, 0xff3F51B5
)

private val lightPalette = listOf(
    0xffff7961, 0xffd05ce3, 0xff6ec6ff, 0xff80e27e, 0xffa98274, 0xff8eacbb,
    0xffffb84d, 0xfffef075, 0xff7986cb
)

private val darkPalette = listOf(
    0xffba000d, 0xff6a0080, 0xff0069c0, 0xff087f23, 0xff4b2c20, 0xff34515e,
    0xffff7d00, 0xfffbc02d, 0xff303f9f
)

@Parcelize
private enum class PaletteVariant : Parcelable {
    NORMAL, LIGHT, DARK
}

@Preview(showBackground = true, widthDp = 300)
@Preview(showBackground = true, widthDp = 300, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ColorSelectionPreview() {
    TrekMeTheme {
        Column {
            ColorPicker(initColor = 0xffd05ce3)
        }
    }
}