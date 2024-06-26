package com.peterlaurence.trekme.features.mapcreate.presentation.ui.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.wmts.domain.model.tileNumberLimit
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme

@Composable
fun MapSizeLimitRationaleDialog(onDismissRequest: () -> Unit, onSeeOffer: () -> Unit) {
    AlertDialog(
        title = {
            Text(text = stringResource(id = R.string.map_size_limit_title))
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(
                        id = R.string.map_size_limit_rationale
                    ).format(tileNumberLimit),
                    modifier = Modifier.fillMaxWidth(),
                    style = TextStyle(hyphens = Hyphens.Auto),
                    fontWeight = FontWeight.Light
                )

                Spacer(modifier = Modifier.height(16.dp))

                Image(
                    painter = painterResource(id = R.drawable.osmhd),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .graphicsLayer {
                            scaleX = 1.5f
                            scaleY = 1.5f
                        },
                    contentDescription = null
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(
                        id = R.string.map_size_limit_rationale_2
                    ).format(tileNumberLimit),
                    modifier = Modifier.fillMaxWidth(),
                    style = TextStyle(hyphens = Hyphens.Auto),
                    fontWeight = FontWeight.Light
                )
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = onSeeOffer
            ) {
                Text(stringResource(id = R.string.see_offer))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.cancel_dialog_string))
            }
        }
    )
}

@Preview
@Composable
private fun DialogPreview() {
    TrekMeTheme {
        MapSizeLimitRationaleDialog({}, {})
    }
}