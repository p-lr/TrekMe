package com.peterlaurence.trekme.features.shop.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState
import com.peterlaurence.trekme.core.billing.domain.model.SubscriptionDetails
import com.peterlaurence.trekme.core.billing.domain.model.TrialAvailable
import com.peterlaurence.trekme.features.common.presentation.ui.pager.HorizontalPager
import com.peterlaurence.trekme.features.common.presentation.ui.pager.PagerState
import com.peterlaurence.trekme.features.common.presentation.ui.pager.rememberPagerState
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.shop.presentation.ui.offers.*
import com.peterlaurence.trekme.features.shop.presentation.viewmodel.ExtendedOfferViewModel
import com.peterlaurence.trekme.features.shop.presentation.viewmodel.GpsProPurchaseViewModel
import java.util.*

@Composable
fun ShopStateful(
    extendedOfferViewModel: ExtendedOfferViewModel = viewModel(),
    gpsProPurchaseViewModel: GpsProPurchaseViewModel = viewModel()
) {
    val extendedOfferPurchaseState by extendedOfferViewModel.purchaseFlow.collectAsState()
    val monthlySubDetails by extendedOfferViewModel.monthlySubscriptionDetailsFlow.collectAsState()
    val yearlySubDetails by extendedOfferViewModel.yearlySubscriptionDetailsFlow.collectAsState()

    val gpsProPurchaseState by gpsProPurchaseViewModel.purchaseFlow.collectAsState()
    val subDetails by gpsProPurchaseViewModel.subscriptionDetailsFlow.collectAsState()

    ShopUi(
        extendedOfferPurchaseState = extendedOfferPurchaseState,
        monthlySubDetails = monthlySubDetails,
        yearlySubDetails = yearlySubDetails,
        gpsProPurchaseState = gpsProPurchaseState,
        subDetails = subDetails,
        onExtendedMonthlyPurchase = extendedOfferViewModel::buyMonthly,
        onExtendedYearlyPurchase = extendedOfferViewModel::buyYearly,
        onGpsProPurchase = gpsProPurchaseViewModel::buy,
        onMainMenuClick = extendedOfferViewModel::onMainMenuClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopUi(
    extendedOfferPurchaseState: PurchaseState,
    monthlySubDetails: SubscriptionDetails?,
    yearlySubDetails: SubscriptionDetails?,
    gpsProPurchaseState: PurchaseState,
    subDetails: SubscriptionDetails?,
    onExtendedMonthlyPurchase: () -> Unit,
    onExtendedYearlyPurchase: () -> Unit,
    onGpsProPurchase: () -> Unit,
    onMainMenuClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.shop_menu_title)) },
                navigationIcon = {
                    IconButton(onClick = onMainMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = "")
                    }
                }
            )
        }
    ) { paddingValues ->
        ShopCarousel(
            modifier = Modifier.padding(paddingValues),
            pagerState = rememberPagerState(),
            firstOffer = OfferUi(
                header = {
                    ExtendedOfferHeader(
                        extendedOfferPurchaseState,
                        monthlySubDetails,
                        yearlySubDetails
                    )
                },
                content = { TrekMeExtendedContent() },
                footerButtons = {
                    ExtendedOfferFooter(
                        extendedOfferPurchaseState,
                        monthlySubDetails,
                        yearlySubDetails,
                        onMonthlyPurchase = onExtendedMonthlyPurchase,
                        onYearlyPurchase = onExtendedYearlyPurchase
                    )
                }
            ),
            secondOffer = OfferUi(
                header = { GpsProPurchaseHeader(gpsProPurchaseState, subDetails) },
                content = { GpsProPurchaseContent() },
                footerButtons = {
                    GpsProPurchaseFooter(
                        gpsProPurchaseState,
                        subDetails,
                        onPurchase = onGpsProPurchase
                    )
                }
            ),
        )
    }
}

@Composable
private fun ShopCarousel(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    firstOffer: OfferUi,
    secondOffer: OfferUi,
) {
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (it == pagerState.currentPage) {
                                MaterialTheme.colorScheme.primary
                            } else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                )
                if (it == 0) {
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }
        }
        HorizontalPager(count = 2, state = pagerState) { page ->
            /* The layout to overlay bottom buttons */
            Box(
                contentAlignment = Alignment.BottomCenter
            ) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, bottom = 43.dp)
                ) {
                    Column {
                        when (page) {
                            0 -> (firstOffer.header)()
                            1 -> (secondOffer.header)()
                        }

                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 32.dp, end = 32.dp, bottom = 32.dp)
                                .verticalScroll(scrollState),
                        ) {
                            when (page) {
                                0 -> (firstOffer.content)()
                                1 -> (secondOffer.content)()
                            }
                        }
                    }
                }

                when (page) {
                    0 -> (firstOffer.footerButtons)()
                    1 -> (secondOffer.footerButtons)()
                }
            }
        }
    }
}

data class OfferUi(
    val header: @Composable () -> Unit,
    val content: @Composable ColumnScope.() -> Unit,
    val footerButtons: @Composable () -> Unit
)

@Preview
@Composable
private fun ShopUiPreview() {
    TrekMeTheme {
        ShopUi(
            extendedOfferPurchaseState = PurchaseState.NOT_PURCHASED,
            monthlySubDetails = SubscriptionDetails(
                UUID.randomUUID(),
                price = "4.99€",
                TrialAvailable(3)
            ),
            yearlySubDetails = SubscriptionDetails(
                UUID.randomUUID(),
                price = "15.99€",
                TrialAvailable(5)
            ),
            gpsProPurchaseState = PurchaseState.NOT_PURCHASED,
            subDetails = SubscriptionDetails(UUID.randomUUID(), price = "9.99€", TrialAvailable(3)),
            onExtendedMonthlyPurchase = {},
            onExtendedYearlyPurchase = {},
            onGpsProPurchase = {}) {
        }
    }
}
