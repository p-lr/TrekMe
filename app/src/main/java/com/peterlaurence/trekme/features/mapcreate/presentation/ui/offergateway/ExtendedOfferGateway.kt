package com.peterlaurence.trekme.features.mapcreate.presentation.ui.offergateway

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState
import com.peterlaurence.trekme.features.common.presentation.ui.theme.accentGreen
import com.peterlaurence.trekme.features.common.presentation.ui.theme.dark_accentGreen
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.ExtendedOfferGatewayViewModel
import kotlinx.coroutines.cancel

@Composable
fun ExtendedOfferGatewayStateful(
    viewModel: ExtendedOfferGatewayViewModel = viewModel(),
    onNavigateToWmtsFragment: () -> Unit,
    onNavigateToShop: () -> Unit,
    onBack: () -> Unit
) {
    val extendedOfferWithIgnPurchaseState by viewModel.extendedOfferWithIgnPurchaseStateFlow.collectAsState()
    val extendedOfferPurchaseState by viewModel.extendedOfferPurchaseStateFlow.collectAsState()

    val uiState by remember {
        derivedStateOf {
            val hasTrekmeExtended = extendedOfferPurchaseState == PurchaseState.PURCHASED
            when (extendedOfferWithIgnPurchaseState) {
                PurchaseState.NOT_PURCHASED, PurchaseState.UNKNOWN -> NotPurchased(hasTrekmeExtended)
                else -> Pending
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.extendedOfferWithIgnPurchaseStateFlow.collect {
            if (it == PurchaseState.PURCHASED) {
                onNavigateToWmtsFragment()
                cancel()
            }
        }
    }

    ExtendedOfferGateway(uiState, onNavigateToShop, onBack)
}

private sealed interface UiState
object Pending : UiState
data class NotPurchased(val hasTrekmeExtended: Boolean) : UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtendedOfferGateway(
    uiState: UiState,
    onNavigateToShop: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
                    }
                },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (uiState) {
                is NotPurchased -> SuggestShopNavigation(
                    uiState.hasTrekmeExtended,
                    onNavigateToShop
                )

                Pending -> ShowPending()
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
    )
    Spacer(modifier = Modifier.height(16.dp))
    LinearProgressIndicator()
}

@Composable
private fun SuggestShopNavigation(hasTrekmeExtended: Boolean, navigateToShop: () -> Unit) {
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
        style = TextStyle(textAlign = TextAlign.Center)
    )

    if (hasTrekmeExtended) {
        Text(
            stringResource(id = R.string.offer_gateway_subscription_must_end),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 32.dp),
            color = MaterialTheme.colorScheme.tertiary,
            style = TextStyle(textAlign = TextAlign.Justify, hyphens = Hyphens.Auto)
        )
    } else {
        Text(
            stringResource(id = R.string.offer_gateway_free_trial),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 32.dp),
            color = if (isSystemInDarkTheme()) dark_accentGreen else accentGreen,
            style = TextStyle(textAlign = TextAlign.Center)
        )

        BoxWithConstraints {
            Button(
                onClick = navigateToShop,
                modifier = Modifier.width(maxWidth * 0.6f),
            ) {
                Text(text = stringResource(id = R.string.offer_gateway_navigation_button).uppercase())
            }
        }
    }
}

