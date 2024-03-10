package com.peterlaurence.trekme.features.gpspro.presentation.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.screens.ErrorScreen
import com.peterlaurence.trekme.features.common.presentation.ui.theme.accentGreen
import com.peterlaurence.trekme.features.common.presentation.ui.theme.dark_accentGreen
import com.peterlaurence.trekme.features.gpspro.presentation.ui.components.IconCircle
import com.peterlaurence.trekme.features.gpspro.presentation.viewmodel.BluetoothDeviceStub
import com.peterlaurence.trekme.features.gpspro.presentation.viewmodel.BluetoothState
import com.peterlaurence.trekme.features.gpspro.presentation.viewmodel.BtConnectPermNotGranted
import com.peterlaurence.trekme.features.gpspro.presentation.viewmodel.BtDisabled
import com.peterlaurence.trekme.features.gpspro.presentation.viewmodel.BtNotSupported
import com.peterlaurence.trekme.features.gpspro.presentation.viewmodel.GpsProViewModel
import com.peterlaurence.trekme.features.gpspro.presentation.viewmodel.PairedDeviceList
import com.peterlaurence.trekme.features.gpspro.presentation.viewmodel.Searching
import com.peterlaurence.trekme.util.android.activity

@Composable
fun GpsProStateful(
    viewModel: GpsProViewModel = hiltViewModel(viewModelStoreOwner = LocalContext.current.activity),
    onMainMenuClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val helpUri = stringResource(id = R.string.gps_pro_help_url)

    GpsProUI(
        bluetoothState = viewModel.bluetoothState,
        isHostSelected = viewModel.isHostSelected,
        onHostSelection = viewModel::onHostSelected,
        onBtDeviceSelection = viewModel::onBtDeviceSelection,
        onShowSettings = viewModel::onShowBtDeviceSettings,
        onMainMenuClick = onMainMenuClick,
        onShowHelp = {
            uriHandler.openUri(helpUri)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GpsProUI(
    bluetoothState: BluetoothState,
    isHostSelected: Boolean,
    onHostSelection: () -> Unit,
    onBtDeviceSelection: (BluetoothDeviceStub) -> Unit,
    onShowSettings: () -> Unit,
    onMainMenuClick: () -> Unit,
    onShowHelp: () -> Unit
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
        }
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            HostDeviceLine(
                name = stringResource(id = R.string.internal_gps),
                isHostSelected,
                onHostSelection
            )
            BluetoothUI(bluetoothState, onBtDeviceSelection, onShowSettings)
        }
    }
}

@Composable
fun BluetoothUI(
    bluetoothState: BluetoothState,
    onBtDeviceSelection: (BluetoothDeviceStub) -> Unit,
    onShowSettings: () -> Unit
) {
    when (bluetoothState) {
        is Searching -> SearchingScreen()
        is PairedDeviceList -> {
            Text(
                stringResource(id = R.string.previously_connected_bt_devices),
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                color = Color(0xFF808080),
                fontSize = 11.sp,
                fontWeight = FontWeight.W500,
                style = TextStyle(textIndent = TextIndent(25.sp), letterSpacing = 1.sp * 0.8)
            )
            val listState = rememberScrollState()
            Column(Modifier.verticalScroll(listState)) {
                for (device in bluetoothState.deviceList) {
                    DeviceLine(device, onShowSettings) {
                        onBtDeviceSelection(it)
                    }
                }
            }
        }

        BtDisabled -> ErrorScreen(message = stringResource(id = R.string.gps_pro_bt_disabled))
        BtNotSupported -> ErrorScreen(message = stringResource(id = R.string.gps_pro_bt_notsupported))
        BtConnectPermNotGranted -> ErrorScreen(message = stringResource(id = R.string.gps_pro_bt_perm_not_granted))
    }
}

@Composable
fun SearchingScreen() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(id = R.string.searching_bt_devices))
    }
}

@Composable
fun HostDeviceLine(name: String, isSelected: Boolean, onSelection: () -> Unit) {
    val color = MaterialTheme.colorScheme.tertiary
    Row(Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .clickable { onSelection() }
        .then(
            if (isSelected) {
                Modifier.border(BorderStroke(2.dp, color), RoundedCornerShape(5.dp))
            } else Modifier
        )
        .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircle(backgroundColor = color, 40.dp, R.drawable.phone)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = name)
    }
}

@Composable
fun DeviceLine(
    device: BluetoothDeviceStub,
    onShowSettings: () -> Unit,
    onSelection: (BluetoothDeviceStub) -> Unit,
) {
    val color = if (isSystemInDarkTheme()) dark_accentGreen else accentGreen
    Row(Modifier
        .fillMaxWidth()
        .padding(start = 8.dp, bottom = 4.dp, end = 8.dp)
        .clickable { onSelection(device) }
        .then(
            if (device.isActive) {
                Modifier.border(BorderStroke(2.dp, color), RoundedCornerShape(5.dp))
            } else Modifier
        )
        .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircle(backgroundColor = color, 40.dp, R.drawable.bluetooth)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = device.name)
        if (device.isActive) {
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(1.dp, 24.dp)
                    .background(color = Color.LightGray)
            )
            Icon(imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                modifier = Modifier
                    .clickable { onShowSettings() }
                    .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                tint = MaterialTheme.colorScheme.secondary)
        }
    }
}