package com.peterlaurence.trekme.features.mapcreate.presentation.ui.offergateway

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.billing.common.PurchaseState
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.ExtendedOfferViewModel
import com.peterlaurence.trekme.features.common.presentation.ui.theme.surfaceBackground
import com.peterlaurence.trekme.features.common.presentation.ui.theme.accentColor
import com.peterlaurence.trekme.features.common.presentation.ui.theme.textColor

@Composable
fun ExtendedOfferGatewayStateful(
    viewModel: ExtendedOfferViewModel = viewModel(),
    onNavigateToWmtsFragment: () -> Unit,
    onNavigateToShop: () -> Unit
) {
    val purchaseState by viewModel.purchaseStateFlow.collectAsState()
    if (purchaseState == PurchaseState.PURCHASED) {
        onNavigateToWmtsFragment()
    }
    ExtendedOfferGateway(purchaseState, onNavigateToShop)
}

@Composable
private fun ExtendedOfferGateway(
    purchaseState: PurchaseState,
    onNavigateToShop: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(surfaceBackground())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (purchaseState) {
            PurchaseState.PURCHASE_PENDING, PurchaseState.CHECK_PENDING -> ShowPending()
            PurchaseState.PURCHASED -> { /* Nothing to do */ }
            PurchaseState.UNKNOWN, PurchaseState.NOT_PURCHASED -> {
                SuggestShopNavigation(onNavigateToShop)
            }
        }
    }
}

@Composable
private fun ShowPending() {
    Text(
        text = stringResource(id = R.string.offer_gateway_check_pending),
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 32.dp),
        color = textColor()
    )
    Spacer(modifier = Modifier.height(16.dp))
    LinearProgressIndicator()
}

@Composable
private fun SuggestShopNavigation(navigateToShop: () -> Unit) {
    Image(
        painter = painterResource(id = R.drawable.takamaka),
        contentDescription = null,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)),
    )

    Spacer(modifier = Modifier.height(32.dp))

    Text(
        stringResource(id = R.string.offer_gateway_suggest_navigation),
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 16.dp),
        color = textColor(),
        style = TextStyle(textAlign = TextAlign.Center)
    )

    Text(
        stringResource(id = R.string.offer_gateway_free_trial),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .padding(bottom = 32.dp)
            .alpha(0.87f),
        color = colorResource(id = R.color.colorGreen),
        style = TextStyle(textAlign = TextAlign.Center)
    )

    BoxWithConstraints {
        Button(
            onClick = navigateToShop,
            modifier = Modifier.width(maxWidth * 0.6f),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = accentColor(),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(50)
        ) {
            Text(text = stringResource(id = R.string.offer_gateway_navigation_button).uppercase())
        }
    }
}

