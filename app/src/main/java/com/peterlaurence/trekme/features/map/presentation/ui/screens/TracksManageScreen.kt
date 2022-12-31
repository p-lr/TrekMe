package com.peterlaurence.trekme.features.map.presentation.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.common.presentation.ui.theme.surfaceBackground
import com.peterlaurence.trekme.features.common.presentation.ui.theme.textColor
import com.peterlaurence.trekme.features.map.presentation.viewmodel.TracksManageViewModel2
import com.peterlaurence.trekme.util.parseColor
import kotlinx.coroutines.flow.update

@Composable
fun TracksManageScreen(viewModel: TracksManageViewModel2) {
    val routes by viewModel.getRouteFlow().collectAsState()
    var selectionId: String? by rememberSaveable { mutableStateOf(null) }

    val selectableRoutes by produceState(
        initialValue = routes.map { SelectableRoute(it, false) },
        key1 = routes,
        key2 = selectionId,
        producer = {
            value = routes.map {
                SelectableRoute(it, it.id == selectionId)
            }
        }
    )

    TrackList(
        selectableRoutes = selectableRoutes,
        onRouteClick = {
            selectionId = it.route.id
        }
    )
}

@Composable
private fun TrackList(
    selectableRoutes: List<SelectableRoute>,
    onRouteClick: (SelectableRoute) -> Unit,
    onVisibilityToggle: (SelectableRoute) -> Unit = {}
) {
    LazyColumn {
        itemsIndexed(selectableRoutes, key = { _, it -> it.route.id }) { index, selectableRoute ->
            TrackItem(
                Modifier.clickable {
                    onRouteClick(selectableRoute)
                },
                selectableRoute,
                index,
                onVisibilityToggle = onVisibilityToggle
            )
        }
    }

}

@Composable
private fun TrackItem(
    modifier: Modifier = Modifier,
    selectableRoute: SelectableRoute,
    index: Int,
    onVisibilityToggle: (SelectableRoute) -> Unit = {}
) {
    val visible by selectableRoute.route.visible.collectAsState()
    val color by selectableRoute.route.color.collectAsState()

    Row(
        modifier
            .fillMaxWidth()
            .background(
                if (selectableRoute.isSelected) {
                    if (isSystemInDarkTheme()) Color(0xff3b5072) else Color(0xffc1d8ff)
                } else {
                    if (index % 2 == 1) surfaceBackground() else {
                        if (isSystemInDarkTheme()) Color(0xff3c3c3c) else Color(0x10000000)
                    }
                }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = selectableRoute.route.name ?: "",
            color = textColor(),
            modifier = Modifier.weight(1f)
        )

        Row {
            ColorIndicator(color)
            Spacer(modifier = Modifier.width(10.dp))
            Image(
                painter = if (visible) {
                    painterResource(id = R.drawable.ic_visibility_black_24dp)
                } else {
                    painterResource(id = R.drawable.ic_visibility_off_black_24dp)
                },
                modifier = Modifier.clickable{ onVisibilityToggle(selectableRoute) },
                contentDescription = stringResource(id = R.string.track_visibility_btn),
                colorFilter = ColorFilter.tint(textColor())
            )
        }

    }
}

@Composable
private fun ColorIndicator(color: String, onClick: () -> Unit = {}) {
    val colorContent = remember(color) {
        Color(parseColor(color))
    }
    val background = if (isSystemInDarkTheme()) Color(0xffa9b7c6) else Color.White
    Canvas(modifier = Modifier.size(24.dp).clickable(onClick = onClick)) {
        val r = 10.dp.toPx()
        val r2 = 12.dp.toPx()
        drawCircle(background, r2)
        drawCircle(colorContent, r)
    }
}

private data class SelectableRoute(val route: Route, val isSelected: Boolean)

@Preview(showBackground = true, widthDp = 350)
@Preview(showBackground = true, widthDp = 350, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TrackListPreview() {
    TrekMeTheme {
        var selectableRoutes by remember {
            mutableStateOf(
                listOf(
                    SelectableRoute(Route(id= "id#1", "A route with a really long name and description"), false),
                    SelectableRoute(Route(id= "id#2", "A route2"), true),
                    SelectableRoute(Route(id= "id#3", "A route3"), false),
                )
            )
        }

        TrackList(
            selectableRoutes = selectableRoutes,
            onRouteClick = {
                selectableRoutes = selectableRoutes.map { r ->
                    if (r.route.id == it.route.id) {
                        r.copy(isSelected = !r.isSelected)
                    } else {
                        r.copy(isSelected = false)
                    }
                }
            },
            onVisibilityToggle = {
                it.route.visible.update { v -> !v }
            }
        )
    }
}
