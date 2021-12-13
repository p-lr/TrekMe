package com.peterlaurence.trekme.ui.mapcreate.dialogs

import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.ConfigurationCompat
import androidx.fragment.app.DialogFragment
import androidx.navigation.navGraphViewModels
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.core.mapsource.wmts.getNumberOfTiles
import com.peterlaurence.trekme.core.mapsource.wmts.toSizeInMo
import com.peterlaurence.trekme.core.mapsource.wmts.toTransactionsNumber
import com.peterlaurence.trekme.ui.mapcreate.wmtsfragment.model.Point
import com.peterlaurence.trekme.ui.mapcreate.wmtsfragment.model.toDomain
import com.peterlaurence.trekme.viewmodel.mapcreate.WmtsViewModel
import kotlinx.parcelize.Parcelize
import java.text.NumberFormat


/**
 * This dialog fragment holds the settings of the minimum and maximum zoom level of the map, before
 * it is downloaded.
 */
open class WmtsLevelsDialog : DialogFragment() {
    /* Level thresholds */
    private var minLevel = 1
    private var maxLevel = 18

    /* Default start values - those can be changed from the WmtsSourceBundle */
    private val startMinLevel = 12
    private var startMaxLevel = 16

    private var currentMinLevel = startMinLevel
    private var currentMaxLevel = startMaxLevel

    private lateinit var transactionsTextView: TextView
    private lateinit var mapSizeTextView: TextView
    private var downloadFormDataBundle: DownloadFormDataBundle? = null
    private val wmtsSource: WmtsSource?
        get() = downloadFormDataBundle?.wmtsSource
    private val viewModel: WmtsViewModel by navGraphViewModels(R.id.mapCreationGraph) {
        defaultViewModelProviderFactory
    }

    companion object {
        const val ARG_WMTS_SOURCE = "WmtsLevelsDialog_wmtsSource"

        fun newInstance(downloadFormDataBundle: DownloadFormDataBundle): WmtsLevelsDialog {
            val f = WmtsLevelsDialog()

            // Supply num input as an argument.
            val args = Bundle()
            args.putParcelable(ARG_WMTS_SOURCE, downloadFormDataBundle)
            f.arguments = args

            return f
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_wmts, null)
        downloadFormDataBundle = arguments?.getParcelable(ARG_WMTS_SOURCE)
        downloadFormDataBundle?.also {
            minLevel = it.levelMin
            maxLevel = it.levelMax
            startMaxLevel = it.startMaxLevel
        }

        configureComponents(view)

        return AlertDialog.Builder(requireContext())
                .setTitle(R.string.wmts_settings_dialog)
                .setView(view)
                .setPositiveButton(R.string.download
                ) { _, _ -> onDownloadFormConfirmed() }
                .setNegativeButton(R.string.cancel_dialog_string) { _, _ -> dismiss() }
                .create()
    }

    protected open fun configureComponents(view: View) {
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

        val tileCount = getNumberOfTiles(currentMinLevel, currentMaxLevel, p1.toDomain(), p2.toDomain())

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
     * Provide the view-model all necessary information to start the download.
     */
    private fun onDownloadFormConfirmed() {
        val (p1, p2) = getPointsOfArea()
        wmtsSource?.let { mapSource ->
            viewModel.onDownloadFormConfirmed(mapSource, p1, p2, currentMinLevel, currentMaxLevel)
        }
    }

    private fun getPointsOfArea(): Pair<Point, Point> {
        val data = arguments?.get(ARG_WMTS_SOURCE) as DownloadFormDataBundle
        val p1 = data.p1
        val p2 = data.p2
        return Pair(p1, p2)
    }
}

@Parcelize
data class DownloadFormDataBundle(
    val wmtsSource: WmtsSource,
    val p1: Point,
    val p2: Point,
    val levelMin: Int = 1,
    val levelMax: Int = 18,
    val startMaxLevel: Int = 16
) : Parcelable