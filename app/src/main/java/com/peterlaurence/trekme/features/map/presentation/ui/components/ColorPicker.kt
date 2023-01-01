package com.peterlaurence.trekme.features.map.presentation.ui.components

import android.content.res.Configuration
import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.accentColor
import com.peterlaurence.trekme.features.common.presentation.ui.theme.backgroundColor
import com.peterlaurence.trekme.features.common.presentation.ui.theme.textColor
import kotlinx.parcelize.Parcelize

@Composable
fun ColorPicker(initColor: Long = 0L, onColorPicked: (Long) -> Unit = {}, onCancel: () -> Unit = {}) {
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
    var activeIndex by remember { mutableStateOf(initValues?.second ?: 0) }

    Column(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor())
            .padding(top = 16.dp)
    ) {
        Text(
            stringResource(id = R.string.color_picker_title),
            modifier = Modifier.padding(start = 16.dp, bottom = 24.dp),
            color = textColor(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        val strokeColor = textColor()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                Surface(
                    onClick = { activeIndex = index },
                    shape = CircleShape,
                    color = Color(palette[index]),
                    modifier = Modifier
                        .padding(horizontal = if (index == activeIndex) 0.dp else 3.dp)
                        .size(if (index == activeIndex) 36.dp else 30.dp)
                        .drawBehind {
                            if (index == activeIndex) {
                                drawCircle(strokeColor, 19.dp.toPx(), style = Stroke(3.dp.toPx()))
                            }
                        }
                ) {

                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                Surface(
                    onClick = { activeIndex = 3 + index },
                    shape = CircleShape,
                    color = Color(palette[3 + index]),
                    modifier = Modifier
                        .padding(horizontal = if (3 + index == activeIndex) 0.dp else 3.dp)
                        .size(if (3 + index == activeIndex) 36.dp else 30.dp)
                        .drawBehind {
                            if (3 + index == activeIndex) {
                                drawCircle(strokeColor, 19.dp.toPx(), style = Stroke(3.dp.toPx()))
                            }
                        }
                ) {

                }
            }
        }

        Row(Modifier.padding(start = 16.dp, top = 16.dp)) {
            Text(
                stringResource(id = R.string.color_variant_label),
                modifier = Modifier.padding(top = 13.dp),
                fontWeight = FontWeight.Medium,
                color = textColor()
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
                    Text(
                        text = stringResource(id = R.string.color_variant_normal),
                        color = textColor()
                    )
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
                    Text(
                        text = stringResource(id = R.string.color_variant_light),
                        color = textColor()
                    )
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
                    Text(
                        text = stringResource(id = R.string.color_variant_dark),
                        color = textColor()
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text(
                    text = stringResource(id = R.string.cancel_dialog_string),
                    color = accentColor()
                )
            }
            TextButton(onClick = { onColorPicked(palette[activeIndex]) }) {
                Text(
                    text = stringResource(id = R.string.apply_color_btn),
                    color = accentColor()
                )
            }
        }
    }
}

private fun findColorInPalettes(color: Long): Pair<PaletteVariant, Int>? {
    PaletteVariant.values().forEach { variant ->
        val palette = when(variant) {
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
    0xfff44336, 0xff9c27b0, 0xff2196f3, 0xff4caf50, 0xff755548, 0xff607d8b
)

private val lightPalette = listOf(
    0xffff7961, 0xffd05ce3, 0xff6ec6ff, 0xff80e27e, 0xffa98274, 0xff8eacbb
)

private val darkPalette = listOf(
    0xffba000d, 0xff6a0080, 0xff0069c0, 0xff087f23, 0xff4b2c20, 0xff34515e
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