package com.peterlaurence.trekme.ui.mapcreate.views.events

import com.peterlaurence.trekme.ui.dialogs.SelectDialogEvent
import kotlinx.android.parcel.Parcelize

@Parcelize
class LayerSelectEvent(override val selection: ArrayList<String>) : SelectDialogEvent(selection) {
    fun getSelection(): String {
        return selection.first()
    }
}