package com.peterlaurence.trekme.features.shop.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.features.common.presentation.ui.pager.HorizontalPager
import com.peterlaurence.trekme.features.common.presentation.ui.pager.PagerState
import com.peterlaurence.trekme.features.common.presentation.ui.pager.rememberPagerState
import com.peterlaurence.trekme.features.common.presentation.ui.theme.backgroundVariant
import com.peterlaurence.trekme.features.shop.presentation.ui.offers.*

@Composable
fun ShopStateful() {
    val pagerState = rememberPagerState()

    ShopCarousel(
        pagerState,
        firstOffer = OfferUi(
            header = { ExtendedOfferHeaderStateful() },
            content = { TrekMeExtendedContent() },
            footerButtons = { ExtendedOfferFooterStateful() }
        ),
        secondOffer = OfferUi(
            header = { GpsProPurchaseHeaderStateful() },
            content = { GpsProPurchaseUI() },
            footerButtons = { GpsProPurchaseFooterStateful() }
        ),
    )
}

@Composable
fun ShopCarousel(
    pagerState: PagerState,
    firstOffer: OfferUi,
    secondOffer: OfferUi,
) {
    Column(Modifier.background(backgroundVariant())) {
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
                            } else Color.White
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
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, bottom = 43.dp)
                        .clip(RoundedCornerShape(20.dp))
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
fun ShopStatefulPreview() {
    ShopStateful()
}