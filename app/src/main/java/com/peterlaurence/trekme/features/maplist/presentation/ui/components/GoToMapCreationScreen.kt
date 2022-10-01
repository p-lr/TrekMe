package com.peterlaurence.trekme.features.maplist.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.buttons.OutlinedButtonColored
import com.peterlaurence.trekme.features.common.presentation.ui.theme.accentColor
import com.peterlaurence.trekme.features.common.presentation.ui.theme.textColor

@Composable
internal fun GoToMapCreationScreen(onButtonCLick: (showOnBoarding: Boolean) -> Unit) {
    BoxWithConstraints {
        val maxWidth = maxWidth
        Column(
            Modifier
                .background(MaterialTheme.colors.surface)
                .padding(32.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.create_first_map_question),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp),
                color = textColor()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onButtonCLick(true) },
                modifier = Modifier.width(maxWidth * 0.6f),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorResource(id = R.color.colorAccent),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(50)
            ) {
                Text(text = stringResource(id = R.string.with_onboarding_btn).uppercase())
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButtonColored(
                onClick = { onButtonCLick(false) },
                modifier = Modifier.width(maxWidth * 0.6f),
                color = accentColor(),
                text = stringResource(id = R.string.without_onboarding_btn).uppercase(),
                shape = RoundedCornerShape(50)
            )
        }
    }
}