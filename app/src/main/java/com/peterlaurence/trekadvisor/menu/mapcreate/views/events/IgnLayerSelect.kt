package com.peterlaurence.trekadvisor.menu.mapcreate.views.events

import com.peterlaurence.trekadvisor.menu.dialogs.SelectDialogEvent

class LayerSelectEvent(selection: ArrayList<String>) : SelectDialogEvent(selection) {
    fun getSelection(): String {
        return selection.first()
    }
}