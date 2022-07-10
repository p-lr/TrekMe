package com.peterlaurence.trekme.features.wifip2p.presentation.ui.dialogs

import com.peterlaurence.trekme.features.common.presentation.ui.dialogs.MapChoiceDialog
import com.peterlaurence.trekme.features.wifip2p.presentation.events.WifiP2pEventBus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * A custom [MapChoiceDialog] which uses the [WifiP2pEventBus] to propagate the map selection event.
 */
@AndroidEntryPoint
class MapSelectionForSend : MapChoiceDialog() {
    @Inject
    lateinit var eventBus: WifiP2pEventBus

    override fun onOkPressed(mapId: Int) {
        eventBus.setMapSelected(mapId)
    }
}