package com.peterlaurence.trekme.ui.mapcreate.views.events

import com.peterlaurence.trekme.ui.dialogs.SelectDialogEvent

class LayerSelectEvent(selection: ArrayList<String>) : SelectDialogEvent(selection) {
    fun getSelection(): String {
        return selection.first()
    }
}