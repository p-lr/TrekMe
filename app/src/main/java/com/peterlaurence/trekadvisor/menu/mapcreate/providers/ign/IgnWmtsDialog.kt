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
import com.peterlaurence.trekadvisor.core.mapsource.wmts.Point
import com.peterlaurence.trekadvisor.core.mapsource.wmts.getNumberOfTiles
import com.peterlaurence.trekadvisor.core.mapsource.wmts.getNumberOfTransactions
import com.peterlaurence.trekadvisor.core.mapsource.wmts.getTileIterable
import com.peterlaurence.trekadvisor.menu.mapcreate.components.Area
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * This dialog fragment holds the settings of the minimum and maximum zoom level of the map, before
 * it is downloaded.
 */
class IgnWmtsDialog : DialogFragment() {
    private val startMinLevel = 12
    private val startMaxLevel = 17
    private val minLevel = 1
    private val maxLevel = 18

    private var currentMinLevel = startMinLevel
    private var currentMaxLevel = startMaxLevel

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

        val barMaxLevel = view.findViewById<SeekBar>(R.id.seekBarMaxLevel)
        barMaxLevel.progress = startMaxLevel - minLevel
        barMaxLevel.max = maxLevel - minLevel

        /* The text indicator of the current min level */
        val minLevel = view.findViewById<TextView>(R.id.minLevel)
        minLevel.text = startMinLevel.toString()

        /* The text indicator of the current mex level */
        val maxLevel = view.findViewById<TextView>(R.id.maxLevel)
        maxLevel.text = startMaxLevel.toString()

        barMinLevel.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                currentMinLevel = this@IgnWmtsDialog.minLevel + i
                minLevel.text = currentMinLevel.toString()

                /* If the min level becomes greater than the max level, update the max level */
                if (currentMinLevel > currentMaxLevel) {
                    currentMaxLevel = currentMinLevel
                    maxLevel.text = currentMaxLevel.toString()
                    barMaxLevel.progress = currentMaxLevel - this@IgnWmtsDialog.minLevel
                }

                EventBus.getDefault().post(TransactionCalculationRequest())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        barMaxLevel.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                currentMaxLevel = this@IgnWmtsDialog.minLevel + i
                maxLevel.text = currentMaxLevel.toString()

                /* If the max level becomes smaller than the min level, update the min level */
                if (currentMaxLevel < currentMinLevel) {
                    currentMinLevel = currentMaxLevel
                    minLevel.text = currentMinLevel.toString()
                    barMinLevel.progress = currentMinLevel - this@IgnWmtsDialog.minLevel
                }

                EventBus.getDefault().post(TransactionCalculationRequest())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onTransactionCalculationRequest(event: TransactionCalculationRequest) {
        val area = arguments?.get(ARG_AREA) as Area

        val p1 = Point(area.relativeX1, area.relativeY1)
        val p2 = Point(area.relativeX2, area.relativeY2)

        val tileCount = getNumberOfTiles(currentMinLevel, currentMaxLevel, p1, p2)
        EventBus.getDefault().post(NumberOfTransactions(getNumberOfTransactions(tileCount)))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNumberOfTransactions(event: NumberOfTransactions) {
        println("Number of transactions ${event.number}")
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    class TransactionCalculationRequest
    class NumberOfTransactions(val number: Long)
}