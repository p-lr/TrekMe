package com.peterlaurence.trekme.features.map.app.beacon

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.accentColor
import com.peterlaurence.trekme.features.common.presentation.ui.theme.textColor
import com.peterlaurence.trekme.features.map.app.service.BeaconService
import com.peterlaurence.trekme.util.compose.annotatedStringResource
import kotlinx.coroutines.flow.Flow

/**
 * On Android, when a map has beacons, we:
 * - Ask for background location permission
 * - Start a foreground service to continuously check if we're in the vicinity of a beacon. The
 * service is started even if the permission isn't granted yet, as it can work when the permission
 * is granted after the service startup.
 */
@Composable
fun BeaconServiceLauncher(backgroundLocationRequest: Flow<Unit>) {
    var isShowingBackgroundLocationRationale by rememberSaveable { mutableStateOf(false) }
    val context by rememberUpdatedState(LocalContext.current)

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->

    }

    LaunchedEffect(backgroundLocationRequest) {
        backgroundLocationRequest.collect {
            /* Start the service */
            val intent = Intent(context, BeaconService::class.java)
            context.startService(intent)

            if (Build.VERSION.SDK_INT < 29) return@collect

            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                isShowingBackgroundLocationRationale = true
            }
        }
    }

    if (isShowingBackgroundLocationRationale) {
        val onDismiss = {
            isShowingBackgroundLocationRationale = false
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        AlertDialog(
            text = {
                Text(text = annotatedStringResource(id = R.string.beacon_background_loc_perm), fontSize = 17.sp, color = textColor())
            },
            onDismissRequest = onDismiss,
            buttons = {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = accentColor()),
                        onClick = onDismiss
                    ) {
                        Text(stringResource(id = R.string.ok_dialog))
                    }
                }
            }
        )
    }
}