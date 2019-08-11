package com.peterlaurence.trekme.ui.mapcreate.views

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.peterlaurence.trekme.billing.Billing
import com.peterlaurence.trekme.core.mapsource.MapSourceBundle
import com.peterlaurence.trekme.ui.mapcreate.components.Area
import com.peterlaurence.trekme.viewmodel.mapcreate.IgnLicenseViewModel


/**
 * This dialog fragment holds the settings of the minimum and maximum zoom level of the map, before
 * it is downloaded.
 */
class WmtsLevelsDialogIgn : WmtsLevelsDialog() {
    private lateinit var billing: Billing
    private lateinit var viewModel: IgnLicenseViewModel

    companion object {
        fun newInstance(area: Area, mapSourceBundle: MapSourceBundle): WmtsLevelsDialogIgn {
            val f = WmtsLevelsDialogIgn()

            // Supply num input as an argument.
            val args = Bundle()
            args.putParcelable(ARG_AREA, area)
            args.putParcelable(ARG_MAP_SOURCE, mapSourceBundle)
            f.arguments = args

            return f
        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)

        billing = Billing(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(activity!!).get(IgnLicenseViewModel::class.java)
        viewModel.getIgnLicenseStatus().observe(this, Observer<Boolean> {
            it?.also {
                println("Ign license status $it")
            }
        })
    }

    override fun onStart() {
        super.onStart()

        viewModel.getIgnLicensePurchaseStatus(billing)
        viewModel.getIgnLicenseInfo(billing)
    }
}