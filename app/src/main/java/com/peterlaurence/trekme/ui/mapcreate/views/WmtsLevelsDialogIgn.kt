package com.peterlaurence.trekme.ui.mapcreate.views

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.billing.Billing
import com.peterlaurence.trekme.core.mapsource.MapSourceBundle
import com.peterlaurence.trekme.ui.mapcreate.components.Area
import com.peterlaurence.trekme.viewmodel.mapcreate.IgnLicenseViewModel


/**
 * This dialog fragment holds the settings of the minimum and maximum zoom level of the map, before
 * it is downloaded.
 * In addition, it interacts with [IgnLicenseViewModel] to provide the user the ability to buy the
 * license, if needed.
 */
class WmtsLevelsDialogIgn : WmtsLevelsDialog() {
    private lateinit var billing: Billing
    private lateinit var viewModel: IgnLicenseViewModel

    private lateinit var priceInformation: TextView
    private lateinit var priceValue: TextView
    private lateinit var buyBtn: Button

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
        showPriceIGN()
    }

    /**
     * In addition to to what the base class does, we want to control specific fields related to
     * IGN license.
     */
    override fun configureComponents(view: View) {
        super.configureComponents(view)

        priceInformation = view.findViewById(R.id.price_info)
        priceInformation.text = getString(R.string.ign_buy_license_banner)

        priceValue = view.findViewById(R.id.price_value)

        buyBtn = view.findViewById(R.id.purchase_btn)
        buyBtn.text = getString(R.string.buy_btn)
    }

    private fun showPriceIGN() {
        priceInformation.visibility = View.VISIBLE
        priceValue.visibility = View.VISIBLE
        buyBtn.visibility = View.VISIBLE
    }

    private fun hidePriceIGN() {
        priceInformation.visibility = View.GONE
        priceValue.visibility = View.GONE
        buyBtn.visibility = View.GONE
    }
}