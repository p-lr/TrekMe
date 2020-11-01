package com.peterlaurence.trekme.ui.wifip2p.dialogs

import com.peterlaurence.trekme.ui.dialogs.MapChoiceDialog
import com.peterlaurence.trekme.ui.wifip2p.events.WifiP2pEventBus
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