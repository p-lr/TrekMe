package com.peterlaurence.trekme.features.shop.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.databinding.FragmentShopBinding
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShopFragment : Fragment() {
    private var binding: FragmentShopBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        /* The action bar isn't managed by Compose */
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            show()
            title = getString(R.string.shop_menu_title)
        }

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