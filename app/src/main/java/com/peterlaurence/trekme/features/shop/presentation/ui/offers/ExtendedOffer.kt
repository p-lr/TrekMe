package com.peterlaurence.trekme.features.shop.presentation.ui.offers

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.billing.common.PurchaseState
import com.peterlaurence.trekme.features.shop.presentation.ui.PriceButton
import com.peterlaurence.trekme.features.shop.presentation.viewmodel.ExtendedOfferViewModel

@Composable
fun ColumnScope.TrekMeExtendedContent() {
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
    LineItem(id = R.string.no_ads)
    LineItem(id = R.string.new_features)
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
            tint = Color(0xFF4CAF50)
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
    Row {
        Text("\u2022")
        Text(
            stringResource(id),
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun ExtendedOfferFooterStateful(viewModel: ExtendedOfferViewModel = viewModel()) {
    val purchaseState by viewModel.purchaseFlow.collectAsState()
    val subDetails by viewModel.subscriptionDetailsFlow.collectAsState(initial = null)

    ExtendedOfferFooter(
        purchaseState,
        pricePerMonth = subDetails?.price,
        pricePerYear = subDetails?.price,
        viewModel::buy,
        viewModel::buy
    )
}

@Composable
private fun ExtendedOfferFooter(
    purchaseState: PurchaseState,
    pricePerMonth: String?,
    pricePerYear: String?,
    buyMonthlyOffer: () -> Unit,
    buyYearlyOffer: () -> Unit
) {
    if (purchaseState != PurchaseState.NOT_PURCHASED) return
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
                color = Color(0xFF4CAF50)
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