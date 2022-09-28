package com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs

import android.os.Bundle
import android.os.ParcelUuid
import androidx.fragment.app.DialogFragment
import com.peterlaurence.trekme.features.common.presentation.ui.dialogs.MapChoiceDialog
import com.peterlaurence.trekme.features.record.presentation.events.RecordEventBus
import com.peterlaurence.trekme.util.parcelable
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

/**
 * A custom [MapChoiceDialog] which uses the [RecordEventBus] to propagate the map selection event.
 */
@AndroidEntryPoint
class MapSelectionForImport : MapChoiceDialog() {
    @Inject
    lateinit var eventBus: RecordEventBus

    override fun onOkPressed(mapId: UUID) {
        val recordId = arguments?.parcelable<ParcelUuid>(RECORD_ID)?.uuid ?: return
        eventBus.setMapSelectedForRecord(mapId, recordId)
    }

    companion object {
        @JvmStatic
        fun newInstance(recordId: UUID): DialogFragment {
            val fragment = MapSelectionForImport()
            val args = Bundle()
            args.putParcelable(RECORD_ID, ParcelUuid(recordId))
            fragment.arguments = args
            return fragment
        }
    }
}

private const val RECORD_ID = "record"