package com.peterlaurence.trekme.ui.gpspro

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.billing.common.PurchaseState
import com.peterlaurence.trekme.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.viewmodel.gpspro.GpsProPurchaseViewModel

@Composable
fun GpsProPurchaseUI(purchaseState: PurchaseState, price: String?, buyCb: () -> Unit) {
    when(purchaseState) {
        PurchaseState.CHECK_PENDING -> Text(text = "Checking..")
        PurchaseState.NOT_PURCHASED, PurchaseState.UNKNOWN -> AccessDeniedUI(purchaseState, price, buyCb)
        else -> { /* Nothing to do */ }
    }
}

@Composable
fun AccessDeniedUI(purchaseState: PurchaseState, price: String?, buyCb: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(Modifier
                .weight(1f)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Image(modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .align(Alignment.CenterHorizontally),
                    painter = painterResource(id = R.drawable.gps_ext),
                    contentScale = ContentScale.Inside,
                    contentDescription = null)
        }
        Button(onClick = buyCb, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("${stringResource(id = R.string.buy_btn)} $price")
        }
        Text(purchaseState.name)
    }
}


class GpsProPurchaseUIView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {

    @Composable
    override fun Content() {
        val viewModel: GpsProPurchaseViewModel = viewModel()
        val purchase by viewModel.purchaseFlow.collectAsState()
        val price by viewModel.priceFlow.collectAsState(null)

        TrekMeTheme {
            GpsProPurchaseUI(purchase, price, viewModel::buy)
        }
    }
}