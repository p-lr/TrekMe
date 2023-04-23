package com.peterlaurence.trekme.features.shop.presentation.ui.offers

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.billing.domain.model.*
import com.peterlaurence.trekme.features.common.presentation.ui.theme.accentGreen
import com.peterlaurence.trekme.features.common.presentation.ui.theme.dark_accentGreen
import com.peterlaurence.trekme.features.shop.presentation.ui.components.Header
import com.peterlaurence.trekme.features.shop.presentation.ui.components.PriceButton

@Composable
fun ExtendedOfferHeader(
    purchaseState: PurchaseState,
    monthlySubDetails: SubscriptionDetails?,
    yearlySubDetails: SubscriptionDetails?
) {
    /* Monthly and yearly trials should have the same trial duration */
    val monthlyTrial = monthlySubDetails?.trialInfo
    val yearlyTrial = yearlySubDetails?.trialInfo
    val trialInfo = if (monthlyTrial is TrialAvailable && yearlyTrial is TrialAvailable) {
        monthlyTrial
    } else TrialUnavailable
    ExtendedOfferHeaderUi(purchaseState, trialInfo)
}

@Composable
private fun ExtendedOfferHeaderUi(purchaseState: PurchaseState, trialInfo: TrialInfo) {
    val subTitle = when (purchaseState) {
        PurchaseState.CHECK_PENDING -> stringResource(id = R.string.module_check_pending)
        PurchaseState.PURCHASED -> stringResource(id = R.string.module_owned)
        PurchaseState.NOT_PURCHASED -> {
            when (trialInfo) {
                is TrialAvailable -> stringResource(id = R.string.free_trial).format(trialInfo.trialDurationInDays)
                TrialUnavailable -> null
            }
        }
        PurchaseState.PURCHASE_PENDING -> stringResource(id = R.string.module_check_pending)
        PurchaseState.UNKNOWN -> stringResource(id = R.string.module_error)
    }
    Header(title = stringResource(id = R.string.trekme_extended_offer), subTitle = subTitle)
}

@Composable
fun TrekMeExtendedContent() {
    CheckSeparator()
    TitleRow(R.string.trekme_extended_ign_maps_title)
    LineItem(R.string.trekme_extended_ign_maps_offline)
    LineItem(R.string.trekme_extended_ign_maps_satellite)

    CheckSeparator()
    TitleRow(R.string.trekme_extended_ign_overlay_title)
    LineItem(R.string.trekme_extended_ign_overlay_1)
    LineItem(R.string.trekme_extended_ign_overlay_2)
    LineItem(R.string.trekme_extended_ign_overlay_3)

    CheckSeparator()
    TitleRow(R.string.trekme_extended_specificities)
    LineItem(id = R.string.add_beacons)
    LineItem(id = R.string.create_map_from_track)
    LineItem(id = R.string.center_on_track)
    LineItem(id = R.string.define_elevation_fix)
    LineItem(id = R.string.osm_level_17)
    LineItem(id = R.string.no_ads)

    NotaBene()
}

@Composable
private fun CheckSeparator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painterResource(id = R.drawable.check),
            contentDescription = null,
            tint = if (isSystemInDarkTheme()) dark_accentGreen else accentGreen
        )
    }
}

@Composable
private fun TitleRow(@StringRes id: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(stringResource(id), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun LineItem(@StringRes id: Int) {
    Row(Modifier.padding(bottom = 4.dp)) {
        Text("\u2022")
        Text(
            stringResource(id),
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 8.dp),
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun NotaBene() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Divider(Modifier.width(50.dp))
    }

    Row {
        /* Set alpha to 0.7 for less emphasize */
        Text(stringResource(id = R.string.nb), Modifier.alpha(0.7f))
        Text(
            stringResource(R.string.ign_caution),
            fontSize = 14.sp,
            modifier = Modifier
                .padding(start = 8.dp)
                .alpha(0.7f),
            style = TextStyle(textAlign = TextAlign.Justify)
        )
    }
}

@Composable
fun ExtendedOfferFooter(
    purchaseState: PurchaseState,
    monthlySubDetails: SubscriptionDetails?,
    yearlySubDetails: SubscriptionDetails?,
    onMonthlyPurchase: () -> Unit,
    onYearlyPurchase: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val subscriptionCenterUri = stringResource(id = R.string.subscription_center)

    ExtendedOfferFooterUi(
        purchaseState,
        pricePerMonth = monthlySubDetails?.price,
        pricePerYear = yearlySubDetails?.price,
        buyMonthlyOffer = onMonthlyPurchase,
        buyYearlyOffer = onYearlyPurchase,
        manageSubscriptionCb = {
            uriHandler.openUri(subscriptionCenterUri)
        }
    )
}

@Composable
private fun ExtendedOfferFooterUi(
    purchaseState: PurchaseState,
    pricePerMonth: String?,
    pricePerYear: String?,
    buyMonthlyOffer: () -> Unit,
    buyYearlyOffer: () -> Unit,
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
    } else if (purchaseState == PurchaseState.NOT_PURCHASED) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (pricePerMonth != null) {
                PriceButton(
                    onClick = buyMonthlyOffer,
                    duration = stringResource(id = R.string.one_month),
                    price = pricePerMonth,
                    color = if (isSystemInDarkTheme()) dark_accentGreen else accentGreen
                )
            }
            if (pricePerYear != null) {
                PriceButton(
                    onClick = buyYearlyOffer,
                    duration = stringResource(id = R.string.one_year),
                    price = pricePerYear
                )
            }
        }
    }
}