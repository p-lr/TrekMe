package com.peterlaurence.trekme.ui.gpspro.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.peterlaurence.trekme.databinding.FragmentGpsProBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GpsProFragment : Fragment() {
    private var binding: FragmentGpsProBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentGpsProBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}