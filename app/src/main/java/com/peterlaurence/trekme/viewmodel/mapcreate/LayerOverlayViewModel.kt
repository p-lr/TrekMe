package com.peterlaurence.trekme.viewmodel.mapcreate

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.mapsource.WmtsSource

class LayerOverlayViewModel : ViewModel() {
    fun setSource(wmtsSource: WmtsSource) {
        println("wmtssource is $wmtsSource")
    }
}