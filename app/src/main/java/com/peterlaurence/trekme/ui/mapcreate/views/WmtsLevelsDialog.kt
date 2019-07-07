package com.peterlaurence.trekme.ui.mapcreate.views

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.ConfigurationCompat
import androidx.fragment.app.DialogFragment
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.mapsource.MapSource
import com.peterlaurence.trekme.core.mapsource.MapSourceBundle
import com.peterlaurence.trekme.core.mapsource.wmts.*
import com.peterlaurence.trekme.service.DownloadService
import com.peterlaurence.trekme.service.event.RequestDownloadMapEvent
import com.peterlaurence.trekme.ui.mapcreate.components.Area
import org.greenrobot.eventbus.EventBus
import java.text.NumberFormat


/**
 * This dialog fragment holds the settings of the minimum and maximum zoom level of the map, before
 * it is downloaded.
 */
class WmtsLevelsDialog : DialogFragment() {
    /* Level thresholds */
    private val minLevel = 1
    private val maxLevel = 18

    /* Start values */
    private val startMinLevel = 12
    private val startMaxLevel = 16

    private var currentMinLevel = startMinLevel
    private var currentMaxLevel = startMaxLevel

    private lateinit var transactionsTextView: TextView
    private lateinit var mapSizeTextView: TextView
    private var mapSource: MapSource? = null

    companion object {
        private const val ARG_AREA = "WmtsLevelsDialog_area"
        private const val ARG_MAP_SOURCE = "WmtsLevelsDialog_mapSource"

        fun newInstance(area: Area, mapSourceBundle: MapSourceBundle): WmtsLevelsDialog {
            val f = WmtsLevelsDialog()

            // Supply num input as an argument.
            val args = Bundle()
            args.putParcelable(ARG_AREA, area)
            args.putParcelable(ARG_MAP_SOURCE, mapSourceBundle)
            f.arguments = args

            return f
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_wmts, null)
        mapSource = arguments?.getParcelable<MapSourceBundle>(ARG_MAP_SOURCE)?.mapSource

        configureComponents(view)

        return AlertDialog.Builder(context!!)
                .setTitle(R.string.wmts_settings_dialog)
                .setView(view)
                .setPositiveButton(R.string.download
                ) { _, _ -> onDownloadFormConfirmed() }
                .setNegativeButton(R.string.cancel_dialog_string) { _, _ -> dismiss() }
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

        /* The minus button for the min level */
        val minLevelMinus = view.findViewById<ImageButton>(R.id.min_level_min_btn)
        minLevelMinus.setOnClickListener { barMinLevel.incrementProgressBy(-1) }

        /* The plus button for the max level */
        val minLevelPlus = view.findViewById<ImageButton>(R.id.min_level_plus_btn)
        minLevelPlus.setOnClickListener { barMinLevel.incrementProgressBy(1) }

        /* The minus button for the max level */
        val maxLevelMinus = view.findViewById<ImageButton>(R.id.max_level_min_btn)
        maxLevelMinus.setOnClickListener { barMaxLevel.incrementProgressBy(-1) }

        /* The plus button for the max level */
        val maxLevelPlus = view.findViewById<ImageButton>(R.id.max_level_plus_btn)
        maxLevelPlus.setOnClickListener { barMaxLevel.incrementProgressBy(1) }

        barMinLevel.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                currentMinLevel = this@WmtsLevelsDialog.minLevel + i
                minLevel.text = currentMinLevel.toString()

                /* If the min level becomes greater than the max level, update the max level */
                if (currentMinLevel > currentMaxLevel) {
                    currentMaxLevel = currentMinLevel
                    maxLevel.text = currentMaxLevel.toString()
                    barMaxLevel.progress = currentMaxLevel - this@WmtsLevelsDialog.minLevel
                }

                updateTransactionCount()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        barMaxLevel.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                currentMaxLevel = this@WmtsLevelsDialog.minLevel + i
                maxLevel.text = currentMaxLevel.toString()

                /* If the max level becomes smaller than the min level, update the min level */
                if (currentMaxLevel < currentMinLevel) {
                    currentMinLevel = currentMaxLevel
                    minLevel.text = currentMinLevel.toString()
                    barMinLevel.progress = currentMinLevel - this@WmtsLevelsDialog.minLevel
                }

                updateTransactionCount()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        transactionsTextView = view.findViewById(R.id.transactions_text_view)
        mapSizeTextView = view.findViewById(R.id.map_size_text_view)
    }

    /**
     * This can be done in UI thread as even for billions of tiles the calculation is almost
     * instantly done.
     */
    fun updateTransactionCount() {
        val (p1, p2) = getPointsOfArea()

        val tileCount = getNumberOfTiles(currentMinLevel, currentMaxLevel, p1, p2)

        val numberOfTransactions = tileCount.toTransactionsNumber()

        /* Format the number of transactions according to the current locale */
        val currentLocale = ConfigurationCompat.getLocales(resources.configuration).get(0)
        val numberFormat = NumberFormat.getNumberInstance(currentLocale)
        val formattedNumber = numberFormat.format(numberOfTransactions)
        transactionsTextView.text = formattedNumber

        /* Show the map size in Mo */
        val mapSizeInMo = "${numberFormat.format(tileCount.toSizeInMo())} Mo"
        mapSizeTextView.text = mapSizeInMo
    }

    override fun onStart() {
        super.onStart()
        updateTransactionCount()
    }

    /**
     * We will start the download with the [DownloadService]. A sticky event is posted right before
     * the service is started.
     *
     * WmtsLevelsDialog                            DownloadService
     *                                sticky
     *      RequestDownloadMapEvent   ----->          (event available)
     *      Intent                    ----->          (service start, then process event)
     *
     * Such communication is necessary because the service isn't started synchronously.
     */
    private fun onDownloadFormConfirmed() {
        val (p1, p2) = getPointsOfArea()
        val mapSpec = getMapSpec(currentMinLevel, currentMaxLevel, p1, p2)
        val tileCount = getNumberOfTiles(currentMinLevel, currentMaxLevel, p1, p2)

        mapSource?.let {
            EventBus.getDefault().postSticky(RequestDownloadMapEvent(it, mapSpec.tileSequence,
                    mapSpec.calibrationPoints, tileCount))
        }

        activity?.apply {
            val intent = Intent(baseContext, DownloadService::class.java)
            startService(intent)
        }
    }

    private fun getPointsOfArea(): Pair<Point, Point> {
        val area = arguments?.get(ARG_AREA) as Area
        val p1 = Point(area.relativeX1, area.relativeY1)
        val p2 = Point(area.relativeX2, area.relativeY2)
        return Pair(p1, p2)
    }
}