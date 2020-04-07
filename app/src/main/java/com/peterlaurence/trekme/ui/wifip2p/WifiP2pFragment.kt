package com.peterlaurence.trekme.ui.wifip2p

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.peterlaurence.trekme.databinding.FragmentWifip2pBinding

class WifiP2pFragment : Fragment() {
    private var _binding: FragmentWifip2pBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentWifip2pBinding.inflate(inflater, container, false)
        return binding.root
    }
}