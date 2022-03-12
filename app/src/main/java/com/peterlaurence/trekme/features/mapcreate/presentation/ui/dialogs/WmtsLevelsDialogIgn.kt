package com.peterlaurence.trekme.features.mapcreate.presentation.ui.dialogs

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.billing.common.PurchaseState
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.ExtendedOfferViewModel
import dagger.hilt.android.AndroidEntryPoint


/**
 * This dialog fragment holds the settings of the minimum and maximum zoom level of the map, before
 * it is downloaded.
 * In addition, it interacts with [ExtendedOfferViewModel] to provide the user the ability to buy the
 * license, if needed.
 */
@AndroidEntryPoint
class WmtsLevelsDialogIgn : WmtsLevelsDialog() {
    private val viewModel: ExtendedOfferViewModel by viewModels()
    private lateinit var ignLicensePrice: String

    private lateinit var priceInformation: TextView
    private lateinit var priceValue: TextView
    private lateinit var buyBtn: Button
    private lateinit var helpBtn: ImageButton

    companion object {
        fun newInstance(downloadFormDataBundle: DownloadFormDataBundle): WmtsLevelsDialogIgn {
            val f = WmtsLevelsDialogIgn()

            // Supply num input as an argument.
            val args = Bundle()
            args.putParcelable(ARG_WMTS_SOURCE, downloadFormDataBundle)
            f.arguments = args

            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.purchaseStateFlow.asLiveData().observe(this) {
            it?.also {
                when (it) {
                    PurchaseState.CHECK_PENDING -> {
                        showCheckPending()
                        setDownloadEnabled(false)
                    }
                    PurchaseState.PURCHASED -> {
                        hidePriceIGN()
                        setDownloadEnabled(true)
                    }
                    PurchaseState.NOT_PURCHASED -> {
                        setDownloadEnabled(false)
                    }
                    PurchaseState.PURCHASE_PENDING -> {
                        showPending()
                        setDownloadEnabled(false)
                    }
                    PurchaseState.UNKNOWN -> {
                        showUnknown()
                        setDownloadEnabled(false)
                    }
                }
            }
        }

        viewModel.priceStateFlow.asLiveData().observe(this) {
            it?.also { price ->
                ignLicensePrice = price
                if (PurchaseState.NOT_PURCHASED == viewModel.purchaseStateFlow.value) {
                    showPriceIGN()
                }
            }
        }
    }

    /**
     * In addition to to what the base class does, we want to control specific fields related to
     * IGN module.
     */
    override fun configureComponents(view: View) {
        super.configureComponents(view)

        priceInformation = view.findViewById(R.id.price_info)
        priceValue = view.findViewById(R.id.price_value)

        buyBtn = view.findViewById(R.id.purchase_btn)
        buyBtn.text = getString(R.string.buy_btn)
        buyBtn.setOnClickListener {
            viewModel.buyLicense()
        }

        helpBtn = view.findViewById(R.id.help_license_info)
        helpBtn.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setMessage(R.string.extended_offer_explanation)
            builder.show()
        }
    }

    private fun showCheckPending() {
        priceInformation.visibility = View.GONE
        priceValue.visibility = View.GONE
        buyBtn.visibility = View.GONE
        priceInformation.visibility = View.VISIBLE
        priceInformation.text = getString(R.string.module_check_pending)
    }

    private fun showPriceIGN() {
        priceInformation.visibility = View.VISIBLE
        priceInformation.text = getString(R.string.offer_suggestion)
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
        priceInformation.text = getString(R.string.module_check_unknown)
        buyBtn.visibility = View.GONE
    }

    private fun setDownloadEnabled(enabled: Boolean) {
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = enabled
    }
}