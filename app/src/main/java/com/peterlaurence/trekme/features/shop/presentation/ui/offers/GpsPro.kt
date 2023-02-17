package com.peterlaurence.trekme.features.shop.presentation.ui.offers

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.billing.domain.model.*
import com.peterlaurence.trekme.features.shop.presentation.ui.components.Header
import com.peterlaurence.trekme.features.shop.presentation.ui.components.PriceButton


@Composable
fun GpsProPurchaseHeader(purchaseState: PurchaseState, subDetails: SubscriptionDetails?) {
    val trialInfo = subDetails?.trialInfo
    val subTitle = when (purchaseState) {
        PurchaseState.CHECK_PENDING -> stringResource(id = R.string.module_check_pending)
        PurchaseState.PURCHASED -> stringResource(id = R.string.module_owned)
        PurchaseState.NOT_PURCHASED -> {
            when (trialInfo) {
                is TrialAvailable -> stringResource(id = R.string.free_trial).format(trialInfo.trialDurationInDays)
                TrialUnavailable -> null
                null -> stringResource(id = R.string.module_error)
            }
        }
        PurchaseState.PURCHASE_PENDING -> stringResource(id = R.string.module_check_pending)
        PurchaseState.UNKNOWN -> stringResource(id = R.string.module_error)
    }
    Header(title = stringResource(id = R.string.gps_pro_name), subTitle = subTitle)
}

@Composable
fun ColumnScope.GpsProPurchaseContent() {
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
        fontWeight = FontWeight.Medium,
    )
    Spacer(modifier = Modifier.padding(8.dp))
    Text(
        stringResource(
            id = R.string.gps_pro_pres_content
        ),
        textAlign = TextAlign.Justify,
        fontSize = 14.sp
    )
    Spacer(modifier = Modifier.padding(8.dp))
    Text(
        stringResource(id = R.string.gps_pro_pres_p2_title),
        fontWeight = FontWeight.Medium,
    )
    Spacer(modifier = Modifier.padding(8.dp))
    for (device in supportedDevices) {
        Text(
            "• $device",
            fontSize = 14.sp
        )
    }
    Spacer(modifier = Modifier.padding(8.dp))
    Text(
        stringResource(
            id = R.string.gps_pro_pres_ending
        ),
        textAlign = TextAlign.Justify,
        fontSize = 14.sp
    )
}

private val supportedDevices = listOf(
    "GARMIN GLO2",
    "Dual XGPS150A & SkyPro XGPS160",
    "Bad Elf Flex, GNSS Surveyor, GPS Pro+",
    "Juniper Systems Geode"
)

@Composable
fun GpsProPurchaseFooter(
    purchaseState: PurchaseState,
    subDetails: SubscriptionDetails?,
    onPurchase: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val subscriptionCenterUri = stringResource(id = R.string.subscription_center)

    GpsProPurchaseFooterUi(
        purchaseState,
        price = subDetails?.price,
        onPurchase,
        manageSubscriptionCb = {
            uriHandler.openUri(subscriptionCenterUri)
        }
    )
}

@Composable
private fun GpsProPurchaseFooterUi(
    purchaseState: PurchaseState,
    price: String?,
    buyCb: () -> Unit,
    manageSubscriptionCb: () -> Unit
) {
    if (purchaseState == PurchaseState.PURCHASED) {
        Button(
            onClick = manageSubscriptionCb,
            modifier = Modifier.padding(bottom = 18.dp),
            shape = RoundedCornerShape(50),
        ) {
            Text(
                text = stringResource(id = R.string.manage_subscription_btn),
                letterSpacing = 1.2.sp
            )
        }
    } else if (purchaseState == PurchaseState.NOT_PURCHASED && price != null) {
        PriceButton(
            onClick = buyCb,
            modifier = Modifier.padding(bottom = 16.dp),
            duration = stringResource(id = R.string.one_year),
            price = price
        )
    }
}
