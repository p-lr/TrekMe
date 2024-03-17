package com.peterlaurence.trekme.main.eventhandler

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.util.android.activity
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle

@Composable
fun BillingEventHandler(appEventBus: AppEventBus) {
    val activity = LocalContext.current.activity
    LaunchedEffectWithLifecycle(appEventBus.billingFlow) {
        it.billingClient.launchBillingFlow(activity, it.flowParams)
    }
}