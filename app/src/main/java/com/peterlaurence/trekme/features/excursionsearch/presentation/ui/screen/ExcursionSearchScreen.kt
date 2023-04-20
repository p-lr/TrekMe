package com.peterlaurence.trekme.features.excursionsearch.presentation.ui.screen

import android.Manifest
import android.content.Context
import android.location.LocationManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.location.LocationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionCategory
import com.peterlaurence.trekme.core.geocoding.domain.engine.GeoPlace
import com.peterlaurence.trekme.core.geocoding.domain.engine.POI
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.excursionsearch.presentation.model.ExcursionCategoryChoice
import com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.ExcursionSearchViewModel

@Composable
fun ExcursionSearchStateful(
    viewModel: ExcursionSearchViewModel = hiltViewModel(),
    onNavigateToExcursionMap: () -> Unit,
    onMenuClick: () -> Unit
) {
    var isUsingCurrentLocation by rememberSaveable { mutableStateOf(true) }
    var placeText by rememberSaveable { mutableStateOf("") }

    val geoPlaceList by viewModel.geoPlaceFlow.collectAsState(initial = emptyList())
    val isLoading by viewModel.isGeoPlaceLoading.collectAsState()

    val selectedGeoPlace by viewModel.selectedGeoPlace.collectAsState()
    val searchEnabled by remember {
        derivedStateOf {
            selectedGeoPlace != null || isUsingCurrentLocation
        }
    }

    val excursionCategories = remember { viewModel.getExcursionCategories() }
    var excursionCategoryChoice by rememberSaveable {
        mutableStateOf<ExcursionCategoryChoice>(ExcursionCategoryChoice.Single(ExcursionCategory.OnFoot))
    }

    var isShowingLocationWarning by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager == null || !LocationManagerCompat.isLocationEnabled(locationManager)) {
                // warn location disabled
                isShowingLocationWarning = true
            }
        } else {
            isUsingCurrentLocation = false
        }
    }

    if (isShowingLocationWarning) {
        AlertDialog(
            title = { Text(stringResource(id = R.string.warning_title)) },
            text = { Text(stringResource(id = R.string.location_disabled)) },
            onDismissRequest = { isShowingLocationWarning = false },
            confirmButton = {
                Button(onClick = { isShowingLocationWarning = false }) {
                    Text(text = stringResource(id = R.string.ok_dialog))
                }
            }
        )
    }

    HikeSearchScreen(
        placeText = placeText,
        isUsingCurrentLocation = isUsingCurrentLocation,
        geoPlaceList = geoPlaceList,
        isLoading = isLoading,
        searchEnabled = searchEnabled,
        excursionCategories = excursionCategories,
        excursionCategoryChoice = excursionCategoryChoice,
        onPlaceTextChange = {
            placeText = it
            viewModel.onQueryTextSubmit(it)
        },
        onToggleUseCurrentLocation = {
            isUsingCurrentLocation = !isUsingCurrentLocation
            if (isUsingCurrentLocation) {
                placeText = ""
                viewModel.selectedGeoPlace.value = null
            }
        },
        onMenuClick = onMenuClick,
        onGeoPlaceSelected = {
            isUsingCurrentLocation = false
            placeText = it.name
            viewModel.selectedGeoPlace.value = it
        },
        onExcursionCategoryChoice = { choice ->
            excursionCategoryChoice = choice
        },
        onSearchClicked = { choice ->
            if (isUsingCurrentLocation) {
                launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                viewModel.searchWithLocation(choice)
            } else {
                viewModel.onSearchWithPlace(choice)
            }
            onNavigateToExcursionMap()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HikeSearchScreen(
    placeText: String,
    isUsingCurrentLocation: Boolean,
    geoPlaceList: List<GeoPlace>,
    isLoading: Boolean,
    searchEnabled: Boolean,
    excursionCategories: Array<ExcursionCategory>,
    excursionCategoryChoice: ExcursionCategoryChoice,
    onPlaceTextChange: (String) -> Unit,
    onToggleUseCurrentLocation: () -> Unit,
    onMenuClick: () -> Unit,
    onGeoPlaceSelected: (GeoPlace) -> Unit,
    onExcursionCategoryChoice: (ExcursionCategoryChoice) -> Unit,
    onSearchClicked: (ExcursionCategoryChoice) -> Unit
) {
    var excursionCategoryModal by rememberSaveable { mutableStateOf<ExcursionCategoryChoice?>(null) }
    val excursionCategoryChoiceSt by rememberUpdatedState(excursionCategoryChoice)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.excursion_feature_menu)) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = "")
                    }
                },
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            ExcursionSearchUi(
                placeText = placeText,
                isUsingCurrentLocation = isUsingCurrentLocation,
                geoPlaceList = geoPlaceList,
                isLoading = isLoading,
                searchEnabled = searchEnabled,
                excursionCategoryChoice = excursionCategoryChoice,
                onPlaceTextChange = onPlaceTextChange,
                onToggleUseCurrentLocation = onToggleUseCurrentLocation,
                onGeoPlaceSelected = onGeoPlaceSelected,
                onChooseCategory = {
                    excursionCategoryModal = excursionCategoryChoiceSt
                },
                onSearchClicked = { onSearchClicked(excursionCategoryChoiceSt) }
            )
        }
    }

    excursionCategoryModal?.also { choice ->
        AlertDialog(
            title = { Text(text = stringResource(id = R.string.excursion_title)) },
            text = {
                Column {
                    ExcursionRow(
                        selected = choice == ExcursionCategoryChoice.All,
                        name = stringResource(
                            id = R.string.excursion_all
                        ),
                        onClick = {
                            onExcursionCategoryChoice(ExcursionCategoryChoice.All)
                            excursionCategoryModal = null
                        }
                    )

                    excursionCategories.forEach { category ->
                        ExcursionRow(
                            selected = choice is ExcursionCategoryChoice.Single && choice.choice == category,
                            name = stringResource(getExcursionCategoryName(category)),
                            onClick = {
                                onExcursionCategoryChoice(ExcursionCategoryChoice.Single(category))
                                excursionCategoryModal = null
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            onDismissRequest = { excursionCategoryModal = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.ExcursionSearchUi(
    placeText: String,
    isUsingCurrentLocation: Boolean,
    geoPlaceList: List<GeoPlace>,
    isLoading: Boolean,
    searchEnabled: Boolean,
    excursionCategoryChoice: ExcursionCategoryChoice,
    onPlaceTextChange: (String) -> Unit,
    onToggleUseCurrentLocation: () -> Unit,
    onGeoPlaceSelected: (GeoPlace) -> Unit,
    onChooseCategory: () -> Unit,
    onSearchClicked: () -> Unit
) {
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    TextField(
        modifier = Modifier
            .fillMaxWidth(),
        value = placeText,
        onValueChange = onPlaceTextChange,
        leadingIcon = {
            if (isSearchMode) {
                IconButton(
                    onClick = {
                        isSearchMode = false
                        focusManager.clearFocus()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back_action)
                    )
                }
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_search_24),
                    contentDescription = stringResource(id = R.string.excursion_search_placeholder)
                )
            }
        },
        placeholder = {
            Text(stringResource(id = R.string.excursion_search_placeholder))
        },
        interactionSource = remember { MutableInteractionSource() }
            .also { interactionSource ->
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect {
                        if (it is PressInteraction.Release) {
                            isSearchMode = true
                        }
                    }
                }
            }
    )

    val onGeoPlaceSelection: (GeoPlace) -> Unit = remember {
        {
            isSearchMode = false
            focusManager.clearFocus()
            onGeoPlaceSelected(it)
        }
    }

    if (isSearchMode) {
        /* In this context, intercept physical back gesture */
        BackHandler {
            isSearchMode = false
            focusManager.clearFocus()
        }

        val scrollState = rememberScrollState()
        Box {
            if (isLoading) {
                LinearProgressIndicator(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                for (place in geoPlaceList) {
                    Column(Modifier.clickable { onGeoPlaceSelection(place) }) {
                        Text(
                            text = place.name,
                            Modifier.padding(start = 24.dp, top = 8.dp),
                            fontSize = 17.sp
                        )
                        Text(
                            text = place.locality,
                            Modifier.padding(start = 24.dp, top = 4.dp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Divider(Modifier.padding(top = 8.dp), thickness = 0.5.dp)
                    }
                }
            }
            if (scrollState.value < scrollState.maxValue) {
                Text(text = "...", modifier = Modifier.align(Alignment.BottomEnd))
            }
        }
    } else {
        Row(
            Modifier.clickable(onClick = onToggleUseCurrentLocation),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isUsingCurrentLocation,
                onCheckedChange = { onToggleUseCurrentLocation() })
            Text(
                text = stringResource(id = R.string.use_my_location),
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            value = when (excursionCategoryChoice) {
                ExcursionCategoryChoice.All -> stringResource(
                    id = R.string.excursion_all
                )

                is ExcursionCategoryChoice.Single -> stringResource(
                    getExcursionCategoryName(
                        excursionCategoryChoice.choice
                    )
                )
            },
            onValueChange = { },
            readOnly = true,
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_hiking),
                    contentDescription = stringResource(id = R.string.use_my_location)
                )
            },
            label = {
                Text(stringResource(id = R.string.activity_label))
            },
            placeholder = {
                Text(stringResource(id = R.string.use_my_location))
            },
            interactionSource = remember { MutableInteractionSource() }
                .also { interactionSource ->
                    LaunchedEffect(interactionSource) {
                        interactionSource.interactions.collect {
                            if (it is PressInteraction.Release) {
                                onChooseCategory()
                            }
                        }
                    }
                }
        )

        Button(
            onClick = onSearchClicked,
            enabled = searchEnabled,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 24.dp)
        ) {
            Text(text = stringResource(id = R.string.excursion_search_button))
        }

        Spacer(modifier = Modifier.weight(1f))

        NewFeatureBanner()

        Spacer(modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun NewFeatureBanner() {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Text(
            text = stringResource(id = R.string.excursion_feature_banner),
            modifier = Modifier.padding(16.dp),
            style = LocalTextStyle.current.copy(
                hyphens = Hyphens.Auto,
                lineBreak = LineBreak.Simple.copy(strategy = LineBreak.Strategy.HighQuality)
            ),
            softWrap = true
        )
    }
}

@Composable
private fun ExcursionRow(selected: Boolean, name: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = name)
    }
}

fun getExcursionCategoryName(category: ExcursionCategory): Int {
    return when (category) {
        ExcursionCategory.OnFoot -> R.string.excursion_on_foot
        ExcursionCategory.Bike -> R.string.excursion_by_bike
        ExcursionCategory.Horse -> R.string.excursion_on_horse
        ExcursionCategory.Nautical -> R.string.excursion_nautical
        ExcursionCategory.Aerial -> R.string.excursion_aerial
        ExcursionCategory.Motorised -> R.string.excursion_motorized
    }
}


@Preview(widthDp = 350, heightDp = 600)
@Composable
private fun ExcursionSearchScreenPreview() {
    var placeText by remember { mutableStateOf("") }

    TrekMeTheme {
        HikeSearchScreen(
            placeText,
            isUsingCurrentLocation = true,
            geoPlaceList = listOf(
                GeoPlace(
                    name = "Eiffel tower",
                    type = POI,
                    locality = "Paris, France",
                    lat = 0.0,
                    lon = 0.0
                ),
                GeoPlace(
                    name = "Niagara falls",
                    type = POI,
                    locality = "Ontario",
                    lat = 0.0,
                    lon = 0.0
                ),
                GeoPlace(
                    name = "Angel falls",
                    type = POI,
                    locality = "Bolívar, Venezuela",
                    lat = 0.0,
                    lon = 0.0
                )
            ),
            isLoading = false,
            searchEnabled = true,
            excursionCategories = arrayOf(ExcursionCategory.OnFoot, ExcursionCategory.Bike),
            excursionCategoryChoice = ExcursionCategoryChoice.All,
            onPlaceTextChange = { placeText = it },
            onToggleUseCurrentLocation = {},
            onMenuClick = {},
            onGeoPlaceSelected = {},
            onExcursionCategoryChoice = {},
            onSearchClicked = {}
        )
    }
}