package com.peterlaurence.trekme.ui.gpspro.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.peterlaurence.trekme.databinding.FragmentBtDeviceSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BtDeviceSettingsFragment : Fragment() {
    var binding: FragmentBtDeviceSettingsBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentBtDeviceSettingsBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}