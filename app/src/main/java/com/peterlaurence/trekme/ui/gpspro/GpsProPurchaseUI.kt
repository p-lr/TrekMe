package com.peterlaurence.trekme.ui.gpspro

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.billing.common.PurchaseState
import com.peterlaurence.trekme.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.viewmodel.gpspro.GpsProPurchaseViewModel

@Composable
fun GpsProPurchaseUI(purchaseState: PurchaseState, price: String?, buyCb: () -> Unit) {
    when (purchaseState) {
        PurchaseState.CHECK_PENDING -> PendingScreen()
        PurchaseState.NOT_PURCHASED,
        PurchaseState.PURCHASE_PENDING,
        PurchaseState.UNKNOWN -> AccessDeniedUI(purchaseState, price, buyCb)
        else -> { /* Nothing to do */
        }
    }
}

@Composable
private fun PendingScreen() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator()
        Text(
            text = stringResource(id = R.string.module_check_pending),
            Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun AccessDeniedUI(purchaseState: PurchaseState, price: String?, buyCb: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .padding(start = 24.dp, end = 24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Image(
                modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .align(Alignment.CenterHorizontally),
                painter = painterResource(id = R.drawable.gps_ext),
                contentScale = ContentScale.Inside,
                contentDescription = null
            )
            Text(
                stringResource(id = R.string.gps_pro_pres_p1_title),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Text(stringResource(id = R.string.gps_pro_pres_content), textAlign = TextAlign.Justify)
            Spacer(modifier = Modifier.padding(8.dp))
            Text(
                stringResource(id = R.string.gps_pro_pres_p2_title),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.padding(8.dp))
            for (device in supportedDevices) {
                Text("• $device")
            }
            Spacer(modifier = Modifier.padding(8.dp))
            Text(stringResource(id = R.string.gps_pro_pres_ending), textAlign = TextAlign.Justify)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (purchaseState) {
                PurchaseState.NOT_PURCHASED -> {
                    Button(onClick = buyCb) {
                        Text("${stringResource(id = R.string.buy_btn)} $price")
                    }
                }
                PurchaseState.PURCHASE_PENDING -> {
                    LinearProgressIndicator()
                }
                PurchaseState.UNKNOWN -> {
                    Text(stringResource(id = R.string.module_check_unknown))
                }
                else -> { /* Nothing to do */
                }
            }
        }
    }
}


class GpsProPurchaseUIView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {

    @Composable
    override fun Content() {
//        val viewModel: GpsProPurchaseViewModel = viewModel()
//        val purchase by viewModel.purchaseFlow.collectAsState()
//        val price by viewModel.priceFlow.collectAsState(null)
//
//        TrekMeTheme {
//            GpsProPurchaseUI(purchase, price, viewModel::buy)
//        }
    }
}

private val supportedDevices = listOf(
    "GARMIN GLO2",
    "Dual XGPS150A & SkyPro XGPS160",
    "Bad Elf Flex, GNSS Surveyor, GPS Pro+",
    "Juniper Systems Geode"
)