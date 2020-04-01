package com.peterlaurence.trekme.ui.mapcreate.views

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.billing.ign.Billing
import com.peterlaurence.trekme.core.mapsource.MapSourceBundle
import com.peterlaurence.trekme.ui.mapcreate.components.Area
import com.peterlaurence.trekme.viewmodel.mapcreate.IgnLicenseDetails
import com.peterlaurence.trekme.viewmodel.mapcreate.IgnLicenseViewModel
import com.peterlaurence.trekme.viewmodel.mapcreate.LicenseStatus


/**
 * This dialog fragment holds the settings of the minimum and maximum zoom level of the map, before
 * it is downloaded.
 * In addition, it interacts with [IgnLicenseViewModel] to provide the user the ability to buy the
 * license, if needed.
 */
class WmtsLevelsDialogIgn : WmtsLevelsDialog() {
    private lateinit var billing: Billing
    private val viewModel: IgnLicenseViewModel by activityViewModels()
    private lateinit var ignLicensePrice: String

    private lateinit var priceInformation: TextView
    private lateinit var priceValue: TextView
    private lateinit var buyBtn: Button
    private lateinit var helpBtn: ImageButton

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

        billing = Billing(context, requireActivity())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.getIgnLicenseStatus().observe(this, Observer<LicenseStatus> {
            it?.also {
                when (it) {
                    LicenseStatus.PURCHASED -> {
                        hidePriceIGN()
                        setDownloadEnabled(true)
                    }
                    LicenseStatus.NOT_PURCHASED -> viewModel.getIgnLicenseInfo(billing)
                    LicenseStatus.PENDING -> showPending()
                }
            }
        })

        viewModel.getIgnLicenseDetails().observe(this, Observer<IgnLicenseDetails> {
            it?.also {
                ignLicensePrice = it.price
                showPriceIGN()
            }
        })
    }

    override fun onStart() {
        super.onStart()

        /* Assume that the license isn't acquired */
        setDownloadEnabled(false)

        /* Then check the license status */
        viewModel.getIgnLicensePurchaseStatus(billing)
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
        buyBtn.setOnClickListener {
            viewModel.buyLicense(billing)
        }

        helpBtn = view.findViewById(R.id.help_license_info)
        helpBtn.setOnClickListener {
            val url = "https://github.com/peterLaurence/TrekMe/blob/master/Readme.fr.md#cartes-ign"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
    }

    private fun showPriceIGN() {
        priceInformation.visibility = View.VISIBLE
        priceValue.visibility = View.VISIBLE
        priceValue.text = ignLicensePrice
        buyBtn.visibility = View.VISIBLE
        helpBtn.visibility = View.VISIBLE
        buyBtn.requestFocus()
    }

    private fun hidePriceIGN() {
        priceInformation.visibility = View.GONE
        priceValue.visibility = View.GONE
        buyBtn.visibility = View.GONE
        helpBtn.visibility = View.GONE
    }

    private fun showPending() {
        priceInformation.text = getString(R.string.ign_buy_license_pending)
        buyBtn.visibility = View.GONE
    }

    private fun setDownloadEnabled(enabled: Boolean) {
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = enabled
    }
}