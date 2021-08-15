package com.peterlaurence.trekme.ui.mapcreate.wmtsfragment.components

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.fragment.app.findFragment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.ui.mapcreate.wmtsfragment.GoogleMapWmtsViewFragment
import com.peterlaurence.trekme.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.viewmodel.mapcreate.GoogleMapWmtsViewModel
import com.peterlaurence.trekme.viewmodel.mapcreate.Loading
import com.peterlaurence.trekme.viewmodel.mapcreate.MapReady
import com.peterlaurence.trekme.viewmodel.mapcreate.WmtsState
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun GoogleMapWmtsUI(wmtsState: WmtsState) {
    when(wmtsState) {
        is MapReady -> {
            Column(Modifier.fillMaxSize()) {
                MapUI(state = wmtsState.mapState)
            }
        }
        is Loading -> {

        }
    }
}

class GoogleMapWmtsUiView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {

    @Composable
    override fun Content() {
        val viewModel: GoogleMapWmtsViewModel = viewModel(findFragment<GoogleMapWmtsViewFragment>().requireActivity())
        val state by viewModel.state.collectAsState()

        TrekMeTheme {
            GoogleMapWmtsUI(state)
        }
    }
}