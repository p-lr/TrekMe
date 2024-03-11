package com.peterlaurence.trekme.features.mapcreate.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.offergateway.ExtendedOfferGatewayStateful
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExtendedOfferGatewayFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrekMeTheme {
                    ExtendedOfferGatewayStateful(
                        onNavigateToWmtsFragment = ::navigateToWmtsFragment,
                        onNavigateToShop = ::navigateToShopFragment,
                        onBack = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }

    private fun navigateToWmtsFragment() {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.extendedOfferGatewayFragment) {
            val action =
                ExtendedOfferGatewayFragmentDirections.actionExtendedOfferGatewayFragmentToWmtsFragment()
            navController.navigate(action)
        }
    }

    private fun navigateToShopFragment() {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.extendedOfferGatewayFragment) {
            navController.navigate(R.id.shopFragment)
        }
    }
}