package com.peterlaurence.trekme.features.mapcreate.presentation.ui.dialogs

import android.os.Bundle
import android.os.Parcelable
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.ui.dialogs.SingleSelectDialog
import com.peterlaurence.trekme.features.mapcreate.presentation.events.MapCreateEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@AndroidEntryPoint
class LayerSelectDialog : SingleSelectDialog() {
    @Inject
    lateinit var eventBus: MapCreateEventBus

    var ids: Array<String>? = null

    companion object {
        const val ARG_LAYER_IDS = "ids"

        /**
         * @param ids The list of ids. This list must have the same size as [values]
         * @param values The list of values to display to the user
         * @param valueSelected The pre-selected value
         */
        @JvmStatic
        fun newInstance(
                title: String,
                ids: List<String>,
                values: List<String>,
                valueSelected: String,
        ): LayerSelectDialog {
            val fragment = LayerSelectDialog()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putStringArray(ARG_LAYER_IDS, ids.toTypedArray())
            args.putStringArray(ARG_VALUES, values.toTypedArray())
            args.putString(ARG_VALUE_SELECTED, valueSelected)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ids = arguments?.getStringArray(ARG_LAYER_IDS)
    }

    override fun onSelection(index: Int) {
        val ids = ids ?: return
        if (index >= 0 && index < ids.size) {
            eventBus.postLayerSelectEvent(ids[index])
        }
    }
}

@Parcelize
data class LayerOverlayDataBundle(val wmtsSource: WmtsSource): Parcelable