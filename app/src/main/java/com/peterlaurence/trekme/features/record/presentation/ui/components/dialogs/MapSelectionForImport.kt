package com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.peterlaurence.trekme.features.common.presentation.ui.dialogs.MapChoiceDialog
import com.peterlaurence.trekme.features.record.presentation.events.RecordEventBus
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
        val recordPath = arguments?.getString(RECORD_ID) ?: return
        eventBus.setMapSelectedForRecord(mapId, recordPath)
    }

    companion object {
        @JvmStatic
        fun newInstance(recordPath: String): DialogFragment {
            val fragment = MapSelectionForImport()
            val args = Bundle()
            args.putString(RECORD_ID, recordPath)
            fragment.arguments = args
            return fragment
        }
    }
}

private const val RECORD_ID = "record"