package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme


@Composable
fun CompassComponent(
    degrees: Float,
    onClick: (() -> Unit)?
) {
    if (onClick != null) {
        FloatingActionButton(
            onClick,
            containerColor = Color.White,
            shape = CircleShape
        ) {
            Image(
                modifier = Modifier.rotate(degrees),
                painter = painterResource(id = R.drawable.compass),
                contentDescription = stringResource(id = R.string.compass_fab_desc),
            )
        }
    } else {
        Surface(
            modifier = Modifier
                .semantics { role = Role.Image }
                .size(56.dp),
            shape = CircleShape
        ) {
            Image(
                modifier = Modifier.padding(12.dp).rotate(degrees),
                painter = painterResource(id = R.drawable.compass),
                contentDescription = stringResource(id = R.string.compass_fab_desc),
            )
        }
    }
}

@Preview
@Composable
private fun CompassPreview1() {
    TrekMeTheme {
        CompassComponent(degrees = 45f) {

        }
    }
}

@Preview
@Composable
private fun CompassPreview2() {
    TrekMeTheme {
        CompassComponent(degrees = 45f, onClick = null)
    }
}