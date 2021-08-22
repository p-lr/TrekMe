package com.peterlaurence.trekme.ui.mapcreate.wmtsfragment.components

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.findFragment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.ui.mapcreate.wmtsfragment.GoogleMapWmtsViewFragment
import com.peterlaurence.trekme.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.viewmodel.mapcreate.*
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.DefaultCanvas
import ovh.plrapps.mapcompose.api.fullSize
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState
import kotlin.math.abs
import kotlin.math.min

@Composable
fun GoogleMapWmtsUI(
    modifier: Modifier,
    wmtsState: WmtsState,
    onValidateArea: () -> Unit
) {
    when (wmtsState) {
        is MapReady -> {
            MapUI(state = wmtsState.mapState)
        }
        is Loading -> {

        }
        is AreaSelection -> {
            Box(modifier) {
                MapUI(state = wmtsState.mapState) {
                    val mapState = wmtsState.mapState
                    Area(
                        modifier = Modifier,
                        mapState = mapState,
                        backgroundColor = colorResource(id = R.color.colorBackgroundAreaView),
                        strokeColor = colorResource(id = R.color.colorStrokeAreaView),
                        p1 = with(wmtsState.areaUiController) {
                            Offset(
                                (p1x * mapState.fullSize.width).toFloat(),
                                (p1y * mapState.fullSize.height).toFloat()
                            )
                        },
                        p2 = with(wmtsState.areaUiController) {
                            Offset(
                                (p2x * mapState.fullSize.width).toFloat(),
                                (p2y * mapState.fullSize.height).toFloat()
                            )
                        }
                    )
                }

                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .height(56.dp)
                ) {
                    Button(
                        onClick = onValidateArea,
                        modifier = Modifier
                            .align(Alignment.Center)
                    ) {
                        Text(text = stringResource(id = R.string.validate_area).uppercase())
                    }
                }
            }
        }
        is WmtsError -> { /* TODO */ }
    }
}


@Composable
private fun FabAreaSelection(onToggleArea: () -> Unit) {
    FloatingActionButton(
        onClick = onToggleArea,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_crop_free_white_24dp),
            contentDescription = null
        )
    }
}

@Composable
private fun Area(
    modifier: Modifier,
    mapState: MapState,
    backgroundColor: Color,
    strokeColor: Color,
    p1: Offset,
    p2: Offset
) {
    DefaultCanvas(
        modifier = modifier,
        mapState = mapState
    ) {
        val topLeft = Offset(min(p1.x, p2.x), min(p1.y, p2.y))

        drawRect(
            backgroundColor,
            topLeft = topLeft,
            size = Size(abs(p2.x - p1.x), abs(p2.y - p1.y))
        )
        drawRect(
            strokeColor, topLeft = topLeft, size = Size(abs(p2.x - p1.x), abs(p2.y - p1.y)),
            style = Stroke(width = 1.dp.toPx() / mapState.scale)
        )
    }
}

class GoogleMapWmtsUiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {

    @Composable
    override fun Content() {
        val viewModel: GoogleMapWmtsViewModel =
            viewModel(findFragment<GoogleMapWmtsViewFragment>().requireActivity())
        val state by viewModel.state.collectAsState()

        val events = viewModel.eventListState.toList()
        val scaffoldState: ScaffoldState = rememberScaffoldState()
        val scope = rememberCoroutineScope()

        TrekMeTheme {

            if (events.isNotEmpty()) {
                val ok = stringResource(id = R.string.ok)
                val message = when (events.first()) {
                    WmtsEvent.OUT_OF_BOUNDS -> stringResource(id = R.string.mapcreate_out_of_bounds)
                }

                SideEffect {
                    scope.launch {
                        scaffoldState.snackbarHostState
                            .showSnackbar(message, actionLabel = ok)
                    }
                    viewModel.acknowledgeError()
                }
            }

            Scaffold(
                Modifier.fillMaxSize(),
                scaffoldState = scaffoldState,
                floatingActionButton = {
                    if (state is MapReady || state is AreaSelection) {
                        FabAreaSelection(viewModel::toggleArea)
                    }
                },
            ) {
                GoogleMapWmtsUI(
                    Modifier.fillMaxSize(),
                    state,
                    viewModel::onValidateArea,
                )
            }
        }
    }
}