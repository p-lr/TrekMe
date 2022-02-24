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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
            // Our page content
            Box(
                contentAlignment = Alignment.BottomCenter
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .verticalScroll(scrollState),
                ) {
                    repeat(20) {
                        Text(
                            text = it.toString(),
                            modifier = Modifier.padding(all = 16.dp),
                            style = MaterialTheme.typography.h6,
                        )
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