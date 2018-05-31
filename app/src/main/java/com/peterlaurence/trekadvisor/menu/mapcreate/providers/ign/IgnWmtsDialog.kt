package com.peterlaurence.trekadvisor.menu.mapcreate.providers.ign

import android.app.Dialog
import android.support.v4.app.DialogFragment
import android.os.Bundle
import android.support.v7.app.AlertDialog
import com.peterlaurence.trekadvisor.R
import com.peterlaurence.trekadvisor.menu.mapcreate.components.Area
import android.content.DialogInterface
import android.view.LayoutInflater




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


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_ign_wmts, null)
        return AlertDialog.Builder(context!!)
                .setTitle("Map saving settings")
                .setView(view)
                .setPositiveButton("ok",
                        DialogInterface.OnClickListener { dialog, whichButton -> println("positive lcik") }
                )
                .create()
    }
}