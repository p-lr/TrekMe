package com.peterlaurence.trekme.ui.mapcreate.dialogs

import android.os.Bundle
import com.peterlaurence.trekme.ui.dialogs.SingleSelectDialog
import com.peterlaurence.trekme.ui.mapcreate.events.MapCreateEventBus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LayerSelectDialog : SingleSelectDialog() {
    @Inject
    lateinit var eventBus: MapCreateEventBus

    companion object {

        @JvmStatic
        fun newInstance(title: String, values: List<String>, valueSelected: String): LayerSelectDialog {
            val fragment = LayerSelectDialog()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putStringArrayList(ARG_VALUES, ArrayList(values))
            args.putString(ARG_VALUE_SELECTED, valueSelected)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onSelection(selection: String) {
        eventBus.postLayerSelectEvent(selection)
    }
}