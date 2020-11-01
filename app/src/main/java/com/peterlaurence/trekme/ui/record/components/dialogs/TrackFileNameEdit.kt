package com.peterlaurence.trekme.ui.record.components.dialogs

import android.os.Bundle
import com.peterlaurence.trekme.ui.dialogs.EditFieldDialog
import com.peterlaurence.trekme.ui.record.events.RecordEventBus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TrackFileNameEdit : EditFieldDialog() {
    @Inject
    lateinit var eventBus: RecordEventBus

    companion object {
        @JvmStatic
        fun newInstance(title: String, initialValue: String): TrackFileNameEdit {
            val fragment = TrackFileNameEdit()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putString(ARG_INIT_VALUE, initialValue)
            fragment.arguments = args
            return fragment
        }
    }
    override fun onEditField(initialValue: String, newValue: String) {
        eventBus.postRecordingNameChange(initialValue, newValue)
    }
}