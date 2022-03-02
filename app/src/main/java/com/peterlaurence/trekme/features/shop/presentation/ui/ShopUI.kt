package com.peterlaurence.trekme.features.shop.presentation.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.pager.HorizontalPager
import com.peterlaurence.trekme.features.common.presentation.ui.pager.PagerState
import com.peterlaurence.trekme.features.common.presentation.ui.pager.rememberPagerState
import com.peterlaurence.trekme.features.shop.presentation.ui.offers.GpsProPurchaseFooterStateful
import com.peterlaurence.trekme.features.shop.presentation.ui.offers.GpsProPurchaseHeaderStateful
import com.peterlaurence.trekme.features.shop.presentation.ui.offers.GpsProPurchaseUI
import com.peterlaurence.trekme.features.shop.presentation.viewmodel.ShopViewModel

@Composable
fun ShopStateful(viewModel: ShopViewModel = viewModel()) {
    val pagerState = rememberPagerState()

    ShopCarousel(
        pagerState,
        firstOffer = OfferUi(
            header = { },
            content = { FirstOfferContent() },
            footerButtons = { FirstOfferButtons() }
        ),
        secondOffer = OfferUi(
            header = { GpsProPurchaseHeaderStateful() },
            content = { GpsProPurchaseUI() },
            footerButtons = { GpsProPurchaseFooterStateful() }
        ),
    )
}

private enum class ShopType {
    TrekMeIntegral, GpsPro
}

@Composable
fun ShopCarousel(
    pagerState: PagerState,
    firstOffer: OfferUi,
    secondOffer: OfferUi,
) {
    Column(Modifier.background(Color.Gray)) {
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
                                Color(0xFFEF6C00)
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, bottom = 43.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                ) {
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

                when (page) {
                    0 -> (firstOffer.footerButtons)()
                    1 -> (secondOffer.footerButtons)()
                }
            }
        }
    }
}

@Composable
fun Header(title: String, subTitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF448AFF),
                            Color(0xFF40C4FF)
                        ),
                        center = Offset.Zero,
                        radius = size.width
                    ),
                    size = size
                )
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title, //stringResource(id = R.string.trekme_extended_offer),
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp
        )
        Text(
            subTitle, //stringResource(id = R.string.trekme_extended_trial),
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ColumnScope.FirstOfferContent() {
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
private fun FirstOfferButtons() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PriceButton(onClick = { /*TODO*/ }, duration = "1 mois", price = "2.99€", color = Color(0xFF4CAF50))
        PriceButton(onClick = { /*TODO*/ }, duration = "1 an", price = "15.99€")
    }
}

@Composable
fun PriceButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    duration: String,
    price: String,
    color: Color = Color(0xFF448AFF),
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = color,
            contentColor = Color.White
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(duration)
            Text(price)
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