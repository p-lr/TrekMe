package com.peterlaurence.trekme.ui.gpspro

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.ui.gpspro.components.IconCircle
import com.peterlaurence.trekme.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.viewmodel.gpspro.*


@Composable
fun GpsProUI(
        bluetoothState: BluetoothState,
        onStartSearch: () -> Unit
) {
    Column {
        Spacer(modifier = Modifier.height(16.dp))
        IconCircle(backgroundColor = Color(0xFF4CAF50), R.drawable.bluetooth)
        Text(
                stringResource(id = R.string.previously_connected_bt_devices),
                color = Color(0xFF808080),
                fontSize = 11.sp,
                fontWeight = FontWeight.W500,
                style = TextStyle(textIndent = TextIndent(25.sp), letterSpacing = 1.sp * 0.8)
        )
        when (bluetoothState) {
            is Searching -> Text("Searching")
            is PairedDeviceList -> TODO()
            BtDisabled -> TODO("show rationale (bt is disabled)")
            BtNotSupported -> TODO("show rationale (bt not supported on this device)")
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
            GpsProUI(viewModel.bluetoothState, {})
        }
    }
}