package com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs

import android.os.Bundle
import android.os.ParcelUuid
import com.peterlaurence.trekme.features.common.presentation.ui.dialogs.EditFieldDialog
import com.peterlaurence.trekme.features.record.presentation.events.RecordEventBus
import com.peterlaurence.trekme.util.android.parcelable
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class TrackFileNameEdit : EditFieldDialog() {
    @Inject
    lateinit var eventBus: RecordEventBus

    companion object {
        const val ARG_ID = "id"

        @JvmStatic
        fun newInstance(title: String, id: UUID, initialValue: String): TrackFileNameEdit {
            val fragment = TrackFileNameEdit()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putParcelable(ARG_ID, ParcelUuid(id))
            args.putString(ARG_INIT_VALUE, initialValue)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onEditField(initialValue: String, newValue: String) {
        val id = arguments?.parcelable<ParcelUuid>(ARG_ID)?.uuid
        if (id != null) {
            eventBus.postRecordingNameChange(id, newValue)
        }
    }
}