package com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.core.geocoding.domain.engine.GeoPlace
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.GeoplaceList

@Composable
fun GeoPlaceListUI(
    modifier: Modifier = Modifier,
    uiState: GeoplaceList,
    isSearchPending: Boolean,
    onGeoPlaceSelection: (GeoPlace) -> Unit
) {
    Box(modifier) {
        LazyColumn(Modifier) {
            items(uiState.geoPlaceList) { place ->
                Column(Modifier.clickable { onGeoPlaceSelection(place) }) {
                    Text(
                        text = place.name,
                        Modifier.padding(start = 24.dp, top = 8.dp),
                        fontSize = 17.sp
                    )
                    Text(text = place.locality, Modifier.padding(start = 24.dp, top = 4.dp))
                    HorizontalDivider(Modifier.padding(top = 8.dp), thickness = 0.5.dp)
                }
            }
        }

        AnimatedVisibility(
            visible = isSearchPending,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            CircularProgressIndicator(
                Modifier
                    .padding(top = 64.dp)
                    .size(25.dp)
                    .background(MaterialTheme.colorScheme.surface, shape = CircleShape)
                    .padding(4.dp),
                strokeWidth = 2.dp
            )
        }
    }
}