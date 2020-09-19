package com.peterlaurence.trekme.ui.mapcreate.views

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.mapsource.WmtsSourceBundle
import com.peterlaurence.trekme.ui.mapcreate.components.Area
import com.peterlaurence.trekme.viewmodel.mapcreate.IgnLicenseViewModel
import com.peterlaurence.trekme.viewmodel.mapcreate.LicenseStatus


/**
 * This dialog fragment holds the settings of the minimum and maximum zoom level of the map, before
 * it is downloaded.
 * In addition, it interacts with [IgnLicenseViewModel] to provide the user the ability to buy the
 * license, if needed.
 */
class WmtsLevelsDialogIgn : WmtsLevelsDialog() {
    private val viewModel: IgnLicenseViewModel by activityViewModels()
    private lateinit var ignLicensePrice: String

    private lateinit var priceInformation: TextView
    private lateinit var priceValue: TextView
    private lateinit var buyBtn: Button
    private lateinit var helpBtn: ImageButton

    companion object {
        fun newInstance(area: Area, wmtsSourceBundle: WmtsSourceBundle): WmtsLevelsDialogIgn {
            val f = WmtsLevelsDialogIgn()

            // Supply num input as an argument.
            val args = Bundle()
            args.putParcelable(ARG_AREA, area)
            args.putParcelable(ARG_WMTS_SOURCE, wmtsSourceBundle)
            f.arguments = args

            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.getIgnLicenseStatus().observe(this) {
            it?.also {
                when (it) {
                    LicenseStatus.PURCHASED -> {
                        hidePriceIGN()
                        setDownloadEnabled(true)
                    }
                    LicenseStatus.NOT_PURCHASED -> viewModel.getIgnLicenseInfo()
                    LicenseStatus.PENDING -> showPending()
                    LicenseStatus.UNKNOWN -> showUnknown()
                }
            }
        }

        viewModel.getIgnLicenseDetails().observe(this) {
            it?.also {
                ignLicensePrice = it.price
                showPriceIGN()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        /* Assume that the license isn't acquired */
        setDownloadEnabled(false)

        /* Then check the license status */
        viewModel.getIgnLicensePurchaseStatus()
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
            viewModel.buyLicense()
        }

        helpBtn = view.findViewById(R.id.help_license_info)
        helpBtn.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setMessage(R.string.ign_license_explanation)
            builder.show()
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
        priceInformation.visibility = View.VISIBLE
        priceInformation.text = getString(R.string.ign_buy_license_pending)
        buyBtn.visibility = View.GONE
    }

    private fun showUnknown() {
        priceInformation.visibility = View.VISIBLE
        priceInformation.text = getString(R.string.ign_buy_license_unknown)
        buyBtn.visibility = View.GONE
    }

    private fun setDownloadEnabled(enabled: Boolean) {
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = enabled
    }
}