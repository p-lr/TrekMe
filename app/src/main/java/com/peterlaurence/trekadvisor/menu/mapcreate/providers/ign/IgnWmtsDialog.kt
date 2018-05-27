package com.peterlaurence.trekadvisor.menu.mapcreate.providers.ign

import android.support.v4.app.DialogFragment
import android.os.Bundle
import com.peterlaurence.trekadvisor.menu.mapcreate.components.Area


class IgnWmtsDialog : DialogFragment() {
    companion object {
        private val ARG_AREA = "IgnWmtsDialog_area"

        fun newInstance(area: Area): IgnWmtsDialog {
            val f = IgnWmtsDialog()

            // Supply num input as an argument.
            val args = Bundle()
            args.putParcelable(ARG_AREA, area)
            f.arguments = args

            return f
        }
    }

}