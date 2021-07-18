package com.peterlaurence.trekme.ui.gpspro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.peterlaurence.trekme.databinding.FragmentGpsProPurchaseBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Before navigating to the "GpsPro" fragment, this fragment provides necessary UI when the user
 * hasn't purchased the module yet.
 */
@AndroidEntryPoint
class GpsProPurchaseFragment : Fragment() {
    private var binding: FragmentGpsProPurchaseBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentGpsProPurchaseBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}