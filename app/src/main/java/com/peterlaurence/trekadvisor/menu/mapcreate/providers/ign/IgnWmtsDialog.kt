package com.peterlaurence.trekadvisor.menu.mapcreate.providers.ign

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import com.peterlaurence.trekadvisor.R
import com.peterlaurence.trekadvisor.menu.mapcreate.components.Area

/**
 * This dialog fragment holds the settings of the minimum and maximum zoom level of the map, before
 * it is downloaded.
 */
class IgnWmtsDialog : DialogFragment() {
    private val startMinLevel = 12
    private val minLevel = 1
    private val maxLevel = 18

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

        configureComponents(view)

        return AlertDialog.Builder(context!!)
                .setTitle(R.string.ign_wmts_settings_dialog)
                .setView(view)
                .setPositiveButton(R.string.ok_dialog,
                        DialogInterface.OnClickListener { dialog, whichButton -> println("positive click") }
                )
                .create()
    }

    private fun configureComponents(view: View) {
        val barMinLevel = view.findViewById<SeekBar>(R.id.seekBarMinLevel)
        barMinLevel.progress = startMinLevel - minLevel
        barMinLevel.max = maxLevel - minLevel

        val minLevel = view.findViewById<TextView>(R.id.minLevel)
        minLevel.text = startMinLevel.toString()

        barMinLevel.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            var progressChanged = this@IgnWmtsDialog.minLevel

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                progressChanged = this@IgnWmtsDialog.minLevel + i
                minLevel.text = progressChanged.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })
    }
}