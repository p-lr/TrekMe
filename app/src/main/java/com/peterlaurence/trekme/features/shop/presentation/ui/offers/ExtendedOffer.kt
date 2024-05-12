package com.peterlaurence.trekme.features.shop.presentation.ui.offers

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
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
fun ExtendedOfferHeaderPurchased() {
    Header(title = stringResource(id = R.string.trekme_extended_offer), subTitle = stringResource(id = R.string.module_owned))
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
fun TrekMeExtendedContent(withIgn: Boolean, purchased: Boolean, onIgnSelectionChanged: (Boolean) -> Unit) {
    CheckSeparator()
    TitleRow(R.string.trekme_extended_maps_title)
    LineItem(R.string.trekme_extended_osm_hd_desc)
    LineItem(id = R.string.no_download_limit)
    LineItem(id = R.string.osm_level_17)

    if (!purchased) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp)
                .clickable { onIgnSelectionChanged(!withIgn) }
        ) {
            Checkbox(checked = withIgn, onCheckedChange = onIgnSelectionChanged)
            Text(
                text = stringResource(id = R.string.trekme_extended_activate_ign),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    if (withIgn) {
        CheckSeparator()
        TitleRow(R.string.trekme_extended_ign_maps_title)
        LineItem(R.string.trekme_extended_ign_maps_offline)
        LineItem(R.string.trekme_extended_ign_maps_satellite)
        LineItem(R.string.trekme_extended_ign_overlay_desc)
    }

    CheckSeparator()
    TitleRow(R.string.trekme_extended_advanced_ft)
    LineItem(id = R.string.track_follow_shop)
    LineItem(id = R.string.trekme_extended_update_maps)
    LineItem(id = R.string.add_beacons)
    LineItem(id = R.string.create_map_from_track)
    LineItem(id = R.string.center_on_track)
    LineItem(id = R.string.define_elevation_fix)

    LineItem(id = R.string.no_ads)

    if (withIgn) {
        NotaBene()
    }
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
        Text(stringResource(id), fontWeight = FontWeight.Medium, style = TextStyle(hyphens = Hyphens.Auto))
    }
}

@Composable
private fun LineItem(@StringRes id: Int) {
    Row(
        Modifier.padding(horizontal = 32.dp),
    ) {
        Text("\u2022", modifier = Modifier.alignByBaseline())
        Text(
            stringResource(id),
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 8.dp).alignByBaseline(),
            lineHeight = 18.sp,
            style = TextStyle(hyphens = Hyphens.Auto)
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
        Text(stringResource(id = R.string.nb),
            Modifier
                .alpha(0.7f)
                .padding(start = 32.dp))
        Text(
            stringResource(R.string.ign_caution),
            fontSize = 14.sp,
            modifier = Modifier
                .padding(start = 8.dp, end = 32.dp)
                .alpha(0.7f),
            style = TextStyle(textAlign = TextAlign.Justify, hyphens = Hyphens.Auto)
        )
    }
}

@Composable
fun ExtendedOfferFooterNotPurchased(
    monthlySubDetails: SubscriptionDetails?,
    yearlySubDetails: SubscriptionDetails?,
    onMonthlyPurchase: () -> Unit,
    onYearlyPurchase: () -> Unit
) {
    ExtendedOfferFooterNotPurchased(
        pricePerMonth = monthlySubDetails?.price,
        pricePerYear = yearlySubDetails?.price,
        buyMonthlyOffer = onMonthlyPurchase,
        buyYearlyOffer = onYearlyPurchase,
    )
}

@Composable
fun ExtendedOfferFooterPurchased() {
    val uriHandler = LocalUriHandler.current
    val subscriptionCenterUri = stringResource(id = R.string.subscription_center)

    Button(
        onClick = {
            uriHandler.openUri(subscriptionCenterUri)
        },
        modifier = Modifier.padding(bottom = 18.dp),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = stringResource(id = R.string.manage_subscription_btn),
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun ExtendedOfferFooterNotPurchased(
    pricePerMonth: String?,
    pricePerYear: String?,
    buyMonthlyOffer: () -> Unit,
    buyYearlyOffer: () -> Unit,
) {
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
