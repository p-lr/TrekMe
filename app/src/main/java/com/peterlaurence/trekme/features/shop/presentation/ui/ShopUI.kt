package com.peterlaurence.trekme.features.shop.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.pager.HorizontalPager
import com.peterlaurence.trekme.features.common.presentation.ui.pager.PagerState
import com.peterlaurence.trekme.features.common.presentation.ui.pager.rememberPagerState
import com.peterlaurence.trekme.features.shop.presentation.viewmodel.ShopViewModel

@Composable
fun ShopStateful(viewModel: ShopViewModel = viewModel()) {
    val pagerState = rememberPagerState()

    ShopCarousel(pagerState)
}

private enum class ShopType {
    TrekMeIntegral, GpsPro
}

@Composable
fun ShopCarousel(pagerState: PagerState) {
    Column(Modifier.background(Color.Gray)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(2) {
                Box(modifier = Modifier
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
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                ) {
                    Header()

                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        repeat(20) {
                            Text(
                                text = it.toString(),
                                modifier = Modifier.padding(all = 16.dp),
                                style = MaterialTheme.typography.h6,
                            )
                        }
                    }
                }

                when (page) {
                    0 -> FirstOfferButtons()
                    1 -> SecondOfferButtons()
                }
            }
        }
    }
}

@Composable
private fun Header() {
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
            stringResource(id = R.string.trekme_extended_offer),
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp
        )
        Text(
            stringResource(id = R.string.trekme_extended_trial),
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun FirstOfferButtons() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = { /*TODO*/ },
            modifier = Modifier.padding(bottom = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF4CAF50),
                contentColor = Color.White
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("1 mois")
                Text("2.99€")
            }
        }

        Button(
            onClick = { /*TODO*/ },
            modifier = Modifier.padding(bottom = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF448AFF),
                contentColor = Color.White
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("1 an")
                Text("15.99€")
            }
        }
    }
}

@Composable
private fun SecondOfferButtons() {
    Button(
        onClick = { /*TODO*/ },
        modifier = Modifier.padding(bottom = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color(0xFF448AFF),
            contentColor = Color.White
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("1 an")
            Text("9.99€")
        }
    }
}