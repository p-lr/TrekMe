package com.peterlaurence.trekme.features.trailsearch.presentation.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.geocoding.domain.engine.City
import com.peterlaurence.trekme.core.geocoding.domain.engine.GeoPlace
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.trailsearch.presentation.model.GeoPlaceAndDistance

@Composable
fun GeoPlaceListComponent(
    geoPlaceList: List<GeoPlaceAndDistance>,
    isLoading: Boolean,
    onGeoPlaceSelection: (GeoPlace) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                Modifier
                    .size(25.dp)
                    .align(Alignment.TopCenter)
                    .background(MaterialTheme.colorScheme.surface, shape = CircleShape)
                    .padding(4.dp),
                strokeWidth = 2.dp
            )
        }
        LazyColumn {
            items(geoPlaceList) { place ->
                if (place.distance != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            Modifier.width(70.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        CircleShape
                                    )
                                    .padding(5.dp),
                                painter = painterResource(id = R.drawable.map_marker_outline),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(
                                    MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.8f
                                    )
                                )
                            )
                            Text(
                                text = UnitFormatter.formatDistance(place.distance, precision = 0),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                        GeoPlaceUi(place.geoPlace, onGeoPlaceSelection)
                    }
                } else {
                    GeoPlaceUi(place.geoPlace, onGeoPlaceSelection)
                }
            }
        }
    }
}

@Composable
private fun GeoPlaceUi(geoPlace: GeoPlace, onGeoPlaceSelection: (GeoPlace) -> Unit) {
    Column(Modifier.clickable { onGeoPlaceSelection(geoPlace) }) {
        Text(
            text = geoPlace.name,
            Modifier.padding(start = 24.dp, top = 8.dp),
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = geoPlace.locality.trim(),
            Modifier.padding(start = 24.dp, top = 4.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )
        Divider(Modifier.padding(top = 8.dp), thickness = 0.5.dp)
    }
}

@Preview(showBackground = true)
@Composable
private fun GeoPlaceListComponentPreview() {
    TrekMeTheme {
        GeoPlaceListComponent(
            geoPlaceList = listOf(
                GeoPlaceAndDistance(
                    GeoPlace(City, "Paris", "75000 France", 12.57, 72.8),
                    15241.0
                ),
                GeoPlaceAndDistance(
                    GeoPlace(City, "Versailles", "78000 France", 12.57, 72.8),
                    17445.0
                )
            ),
            isLoading = false,
            onGeoPlaceSelection = {}
        )
    }
}