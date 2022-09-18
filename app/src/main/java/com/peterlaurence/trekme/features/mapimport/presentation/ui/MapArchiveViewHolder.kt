package com.peterlaurence.trekme.features.mapimport.presentation.ui

import android.graphics.Color
import android.graphics.ColorFilter
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.RecyclerView
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.models.MapParseStatus
import com.peterlaurence.trekme.databinding.MapArchiveCardBinding

/**
 * @author P.Laurence on 22/12/17 -- Converted to Kotlin on 02/10/20
 */
class MapArchiveViewHolder(binding: MapArchiveCardBinding) : RecyclerView.ViewHolder(binding.root) {
    var layout: ConstraintLayout = binding.root
        private set
    var mapArchiveName: TextView = binding.mapArchiveName
        private set

    private val progressBarHorizontal: ProgressBar = binding.unzipProgressbar
    private val iconMapExtracted: ImageView = binding.extractionDone
    private val iconMapExtractionError: ImageView = binding.extractionError
    private val extractionLabel: TextView = binding.extractionTxtview
    private val progressBarIndUnzip: ProgressBar = binding.extractionIndProgressbar
    private val progressBarIndMapCreation: ProgressBar = binding.mapcreationIndProgressbar
    private val iconMapCreated: ImageView = binding.mapcreationDone
    private val mapCreationLabel: TextView = binding.mapcreationTxtview

    private val colorFilter: ColorFilter? by lazy {
        BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.GRAY, BlendModeCompat.MODULATE)
    }

    fun init() {
        progressBarHorizontal.visibility = View.GONE
        progressBarIndUnzip.visibility = View.GONE
        iconMapExtracted.visibility = View.GONE
        iconMapExtractionError.visibility = View.GONE
        extractionLabel.visibility = View.GONE
        progressBarIndMapCreation.visibility = View.GONE
        iconMapCreated.visibility = View.GONE
        mapCreationLabel.visibility = View.GONE
    }

    fun onProgress(p: Int) {
        progressBarHorizontal.visibility = View.VISIBLE
        progressBarIndUnzip.visibility = View.VISIBLE
        extractionLabel.visibility = View.VISIBLE
        progressBarHorizontal.progress = p
    }

    fun onUnzipFinished() {
        progressBarHorizontal.visibility = View.GONE
        progressBarIndUnzip.visibility = View.GONE
        extractionLabel.visibility = View.VISIBLE
        mapCreationLabel.visibility = View.VISIBLE
        iconMapExtracted.visibility = View.VISIBLE
        progressBarIndMapCreation.visibility = View.VISIBLE
    }

    fun onUnzipError() {
        progressBarIndUnzip.visibility = View.GONE
        iconMapExtractionError.visibility = View.VISIBLE
        extractionLabel.setText(R.string.extraction_error)
    }

    fun onMapImported(status: MapParseStatus) {
        when (status) {
            MapParseStatus.NEW_MAP -> mapCreationLabel.setText(R.string.imported_new_map)
            MapParseStatus.EXISTING_MAP -> mapCreationLabel.setText(R.string.imported_untouched)
            MapParseStatus.UNKNOWN_MAP_ORIGIN, MapParseStatus.NO_MAP -> mapCreationLabel.setText(R.string.map_import_error)
        }
        progressBarIndMapCreation.visibility = View.GONE
        iconMapCreated.visibility = View.VISIBLE
        mapCreationLabel.visibility = View.VISIBLE
    }

    init {
        progressBarHorizontal.max = 100
        progressBarIndMapCreation.indeterminateDrawable.colorFilter = colorFilter
        progressBarIndUnzip.indeterminateDrawable.colorFilter = colorFilter
    }
}
