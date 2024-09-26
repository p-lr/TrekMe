package com.peterlaurence.trekme.features.shop.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState
import com.peterlaurence.trekme.core.billing.domain.model.SubscriptionDetails
import com.peterlaurence.trekme.core.billing.domain.model.TrialAvailable
import com.peterlaurence.trekme.features.common.presentation.ui.scrollbar.drawVerticalScrollbar
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.shop.presentation.ui.offers.*
import com.peterlaurence.trekme.features.shop.presentation.viewmodel.ExtendedOfferViewModel
import com.peterlaurence.trekme.features.shop.presentation.viewmodel.ExtendedWithIgnViewModel
import com.peterlaurence.trekme.features.shop.presentation.viewmodel.GpsProPurchaseViewModel
import java.util.*

@Composable
fun ShopStateful(
    extendedWithIgnViewModel: ExtendedWithIgnViewModel = hiltViewModel(),
    extendedOfferViewModel: ExtendedOfferViewModel = hiltViewModel(),
    gpsProPurchaseViewModel: GpsProPurchaseViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val extendedOfferWithIgnPurchaseState by extendedWithIgnViewModel.purchaseFlow.collectAsState()
    val monthlySubDetailsIgn by extendedWithIgnViewModel.monthlySubscriptionDetailsFlow.collectAsState()
    val yearlySubDetailsIgn by extendedWithIgnViewModel.yearlySubscriptionDetailsFlow.collectAsState()

    val extendedOfferPurchaseState by extendedOfferViewModel.purchaseFlow.collectAsState()
    val monthlySubDetails by extendedOfferViewModel.monthlySubscriptionDetailsFlow.collectAsState()
    val yearlySubDetails by extendedOfferViewModel.yearlySubscriptionDetailsFlow.collectAsState()

    var withIgn: Boolean by remember { mutableStateOf(false) }

    val uiState by produceState(
        initialValue = if (extendedOfferWithIgnPurchaseState == PurchaseState.PURCHASED) {
            Purchased(isIgn = true)
        } else if (extendedOfferPurchaseState == PurchaseState.PURCHASED) {
            Purchased(isIgn = false)
        } else {
            Selection(
                isIgn = false,
                extendedOfferPurchaseState,
                monthlySubDetails,
                yearlySubDetails
            )
        },
        key1 = withIgn,
        key2 = extendedOfferPurchaseState,
        key3 = extendedOfferWithIgnPurchaseState
    ) {
        value = if (extendedOfferWithIgnPurchaseState == PurchaseState.PURCHASED) {
            Purchased(isIgn = true)
        } else if (extendedOfferPurchaseState == PurchaseState.PURCHASED) {
            Purchased(isIgn = false)
        } else {
            if (withIgn) {
                Selection(
                    isIgn = true,
                    extendedOfferWithIgnPurchaseState,
                    monthlySubDetailsIgn,
                    yearlySubDetailsIgn
                )
            } else {
                Selection(
                    isIgn = false,
                    extendedOfferPurchaseState,
                    monthlySubDetails,
                    yearlySubDetails
                )
            }
        }
    }

    val gpsProPurchaseState by gpsProPurchaseViewModel.purchaseFlow.collectAsState()
    val subDetails by gpsProPurchaseViewModel.subscriptionDetailsFlow.collectAsState()

    ShopUi(
        uiState = uiState,
        gpsProPurchaseState = gpsProPurchaseState,
        subDetails = subDetails,
        onExtendedMonthlyPurchase = {
            if (withIgn) {
                extendedWithIgnViewModel.buyMonthly()
            } else {
                extendedOfferViewModel.buyMonthly()
            }
        },
        onExtendedYearlyPurchase = {
            if (withIgn) {
                extendedWithIgnViewModel.buyYearly()
            } else {
                extendedOfferViewModel.buyYearly()
            }
        },
        onIgnSelectionChanged = { withIgn = it },
        onGpsProPurchase = gpsProPurchaseViewModel::buy,
        onBackClick = onBackClick
    )
}

private sealed interface UiState {
    val isIgn: Boolean
}
private data class Purchased(override val isIgn: Boolean) : UiState
private data class Selection(
    override val isIgn: Boolean,
    val purchaseState: PurchaseState,
    val monthlySubDetails: SubscriptionDetails?,
    val yearlySubDetails: SubscriptionDetails?
) : UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShopUi(
    uiState: UiState,
    gpsProPurchaseState: PurchaseState,
    subDetails: SubscriptionDetails?,
    onExtendedMonthlyPurchase: () -> Unit,
    onExtendedYearlyPurchase: () -> Unit,
    onIgnSelectionChanged: (Boolean) -> Unit,
    onGpsProPurchase: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.shop_menu_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
                    }
                }
            )
        }
    ) { paddingValues ->
        ShopCarousel(
            modifier = Modifier.padding(paddingValues),
            pagerState = rememberPagerState { 2 },
            firstOffer = OfferUi(
                header = {
                    when (uiState) {
                        is Purchased -> ExtendedOfferHeaderPurchased()
                        is Selection -> ExtendedOfferHeader(
                            uiState.purchaseState,
                            uiState.monthlySubDetails,
                            uiState.yearlySubDetails
                        )
                    }
                },
                content = {
                    val purchased = uiState is Purchased
                    TrekMeExtendedContent(uiState.isIgn, purchased, onIgnSelectionChanged)
                },
                footerButtons = {
                    when (uiState) {
                        is Purchased -> ExtendedOfferFooterPurchased()
                        is Selection -> ExtendedOfferFooterNotPurchased(
                            uiState.monthlySubDetails,
                            uiState.yearlySubDetails,
                            onMonthlyPurchase = onExtendedMonthlyPurchase,
                            onYearlyPurchase = onExtendedYearlyPurchase
                        )
                    }
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
        HorizontalPager(state = pagerState) { page ->
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
                                .padding(bottom = 32.dp)
                                .drawVerticalScrollbar(scrollState)
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
            uiState = Selection(
                isIgn = true,
                purchaseState = PurchaseState.NOT_PURCHASED,
                monthlySubDetails = SubscriptionDetails(
                    UUID.randomUUID(),
                    price = "4.99€",
                    TrialAvailable(3)
                ),
                yearlySubDetails = SubscriptionDetails(
                    UUID.randomUUID(),
                    price = "15.99€",
                    TrialAvailable(5)
                )
            ),
            gpsProPurchaseState = PurchaseState.NOT_PURCHASED,
            subDetails = SubscriptionDetails(UUID.randomUUID(), price = "9.99€", TrialAvailable(3)),
            onExtendedMonthlyPurchase = {},
            onExtendedYearlyPurchase = {},
            onIgnSelectionChanged = {},
            onGpsProPurchase = {}) {
        }
    }
}
