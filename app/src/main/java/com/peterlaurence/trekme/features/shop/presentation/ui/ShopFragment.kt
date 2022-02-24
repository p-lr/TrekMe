package com.peterlaurence.trekme.features.shop.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.databinding.FragmentShopBinding
import com.peterlaurence.trekme.features.shop.presentation.viewmodel.ShopViewModel
import com.peterlaurence.trekme.ui.theme.TrekMeTheme

class ShopFragment : Fragment() {
    private var binding: FragmentShopBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentShopBinding.inflate(inflater, container, false)
        this.binding = binding

        binding.shopScreen.setContent {
            TrekMeTheme {
                ShopStateful()
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}