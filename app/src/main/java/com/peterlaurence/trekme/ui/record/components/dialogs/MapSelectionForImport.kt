package com.peterlaurence.trekme.ui.record.components.dialogs

import com.peterlaurence.trekme.ui.dialogs.MapChoiceDialog
import com.peterlaurence.trekme.ui.record.events.RecordEventBus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * A custom [MapChoiceDialog] which uses the [RecordEventBus] to propagate the map selection event.
 */
@AndroidEntryPoint
class MapSelectionForImport : MapChoiceDialog() {
    @Inject
    lateinit var eventBus: RecordEventBus

    override fun onOkPressed(mapId: Int) {
        eventBus.setMapSelected(mapId)
    }
}