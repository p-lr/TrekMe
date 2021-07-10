package com.peterlaurence.trekme.ui.gpspro

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.viewmodel.gpspro.DeviceFound
import com.peterlaurence.trekme.viewmodel.gpspro.DeviceSearchState
import com.peterlaurence.trekme.viewmodel.gpspro.GpsProViewModel
import com.peterlaurence.trekme.viewmodel.gpspro.Searching


@Composable
fun GpsProUI(
        deviceSearchState: DeviceSearchState,
) {
    Column {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
                stringResource(id = R.string.paired_devices_title),
                color = colorResource(id = R.color.colorAccent),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                style = TextStyle(textIndent = TextIndent(14.sp))
        )
        when (deviceSearchState) {
            is Searching -> Text("Searching")
            is DeviceFound -> TODO()
        }
    }
}

class GpsProUIView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {

    @Composable
    override fun Content() {
        val viewModel: GpsProViewModel = viewModel()

        TrekMeTheme {
            GpsProUI(viewModel.searchState)
        }
    }
}