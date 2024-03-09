package com.peterlaurence.trekme.features.wifip2p.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.dialogs.MapSelectionDialogStateful
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.wifip2p.app.service.AwaitingP2pConnection
import com.peterlaurence.trekme.features.wifip2p.app.service.AwaitingSocketConnection
import com.peterlaurence.trekme.features.wifip2p.app.service.ByUser
import com.peterlaurence.trekme.features.wifip2p.app.service.Loading
import com.peterlaurence.trekme.features.wifip2p.app.service.MapSuccessfullyLoaded
import com.peterlaurence.trekme.features.wifip2p.app.service.P2pConnected
import com.peterlaurence.trekme.features.wifip2p.app.service.SocketConnected
import com.peterlaurence.trekme.features.wifip2p.app.service.Started
import com.peterlaurence.trekme.features.wifip2p.app.service.StopReason
import com.peterlaurence.trekme.features.wifip2p.app.service.Stopped
import com.peterlaurence.trekme.features.wifip2p.app.service.Stopping
import com.peterlaurence.trekme.features.wifip2p.app.service.WifiP2pService
import com.peterlaurence.trekme.features.wifip2p.app.service.WifiP2pServiceErrors
import com.peterlaurence.trekme.features.wifip2p.app.service.WifiP2pState
import com.peterlaurence.trekme.features.wifip2p.app.service.WithError
import com.peterlaurence.trekme.features.wifip2p.presentation.ui.widgets.WaveSearchIndicator
import com.peterlaurence.trekme.features.wifip2p.presentation.viewmodel.ServiceAlreadyStarted
import com.peterlaurence.trekme.features.wifip2p.presentation.viewmodel.WifiNotEnabled
import com.peterlaurence.trekme.features.wifip2p.presentation.viewmodel.WifiP2pViewModel
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * The frontend for the [WifiP2pService].
 * It reacts to some of the [WifiP2pState]s emitted by the service.
 */
@Composable
fun WifiP2pStateful(
    viewModel: WifiP2pViewModel = hiltViewModel(),
    onMainMenuClick: () -> Unit
) {

    val state by viewModel.state.collectAsState()

    val uriHandler = LocalUriHandler.current
    val helpUri = stringResource(id = R.string.wifip2p_help_url)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val wifiError = stringResource(id = R.string.wifip2p_warning_wifi)
    LaunchedEffectWithLifecycle(viewModel.errors) { errors ->
        when (errors) {
            WifiNotEnabled -> {
                scope.launch {
                    snackbarHostState.showSnackbar(wifiError, withDismissAction = true)
                }
            }

            ServiceAlreadyStarted -> {} // nothing for now
        }
    }

    WifiP2pScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onMainMenuClick = onMainMenuClick,
        onShowHelp = {
            uriHandler.openUri(helpUri)
        },
        onReceive = { viewModel.onRequestReceive() },
        onSend = { viewModel.onRequestSend(it) },
        onStop = { viewModel.onRequestStop() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WifiP2pScreen(
    state: WifiP2pState,
    snackbarHostState: SnackbarHostState,
    onMainMenuClick: () -> Unit,
    onShowHelp: () -> Unit,
    onReceive: () -> Unit,
    onSend: (UUID) -> Unit,
    onStop: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.wifip2p_title)) },
                navigationIcon = {
                    IconButton(onClick = onMainMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = "")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onShowHelp,
                        modifier = Modifier.width(36.dp)
                    ) {
                        Icon(
                            painterResource(id = R.drawable.help_circle_outline_white),
                            contentDescription = null,
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Row(
            Modifier.padding(paddingValues),
            horizontalArrangement = Arrangement.Center
        ) {
            when (state) {
                is Stopped -> StoppedCase(state.stopReason, onReceive, onSend)
                else -> ActiveCase(state, onStop)
            }
        }
    }
}

@Composable
private fun ActiveCase(
    state: WifiP2pState,
    onStop: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.weight(0.1f))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = when (state) {
                Started -> stringResource(id = R.string.wifip2p_searching)
                AwaitingP2pConnection, AwaitingSocketConnection, SocketConnected -> {
                    stringResource(id = R.string.wifip2p_device_found)
                }

                P2pConnected -> stringResource(id = R.string.wifip2p_connected)
                is Loading, Stopping -> stringResource(id = R.string.wifip2p_loading)
                is Stopped -> "" // doesn't apply here
            },
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.weight(0.1f))
        if (state == Started) {
            WaveSearchIndicator(Modifier.align(Alignment.CenterHorizontally), isBeating = true)
        }
        if (state is Loading) {
            LinearProgressIndicator(
                progress = { state.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                strokeCap = StrokeCap.Round
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp),
            onClick = onStop,
            colors = ButtonDefaults.buttonColors().copy(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Text(text = stringResource(id = R.string.wifip2p_stop_btn))
        }
    }
}

@Composable
private fun StoppedCase(
    stopReason: StopReason? = null,
    onReceive: () -> Unit,
    onSend: (UUID) -> Unit
) {
    var isShowingMapChoice by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.weight(0.25f))
        Text(text = stringResource(id = R.string.wifip2p_explanation))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(id = R.string.wifip2p_explanation2))
        Spacer(modifier = Modifier.height(24.dp))
        FilledTonalButton(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onReceive,
        ) {
            Text(text = stringResource(id = R.string.wifip2p_rcv_btn))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = { isShowingMapChoice = true }
        ) {
            Text(text = stringResource(id = R.string.wifip2p_send_btn))
        }

        Spacer(modifier = Modifier.weight(0.5f))

        when (stopReason) {
            is MapSuccessfullyLoaded -> {
                Spacer(modifier = Modifier.height(24.dp))
                Image(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    painter = painterResource(id = R.drawable.ic_emoji_party_face_1f973),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.wifip2p_successful_load).format(stopReason.name),
                    textAlign = TextAlign.Center
                )
            }

            is WithError -> {
                Spacer(modifier = Modifier.height(24.dp))

                Image(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    painter = painterResource(id = R.drawable.ic_emoji_disappointed_face_1f61e),
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.wifip2p_error).format(stopReason.error.name),
                    textAlign = TextAlign.Center
                )
            }

            null, ByUser -> {} // nothing to show
        }

        Spacer(modifier = Modifier.weight(0.5f))
    }

    if (isShowingMapChoice) {
        MapSelectionDialogStateful(
            onMapSelected = { map -> onSend(map.id) },
            onDismissRequest = { isShowingMapChoice = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StartedPreview() {
    TrekMeTheme {
        ActiveCase(Started, onStop = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun AwaitingP2pConnectionPreview() {
    TrekMeTheme {
        ActiveCase(AwaitingP2pConnection, onStop = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingPreview() {
    TrekMeTheme {
        ActiveCase(Loading(40), onStop = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun StoppedPreview() {
    TrekMeTheme {
        StoppedCase(stopReason = null, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun StoppedAfterSuccessPreview() {
    TrekMeTheme {
        StoppedCase(stopReason = MapSuccessfullyLoaded(name = "A map"), {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun StoppedAfterErrorPreview() {
    TrekMeTheme {
        StoppedCase(stopReason = WithError(error = WifiP2pServiceErrors.UNZIP_ERROR), {}, {})
    }
}