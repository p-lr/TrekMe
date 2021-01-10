package com.peterlaurence.trekme.ui.mapcreate.overlay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.core.mapsource.WmtsSourceBundle
import com.peterlaurence.trekme.databinding.FragmentLayerOverlayBinding

class LayerOverlayFragment : Fragment() {
    private var wmtsSource: WmtsSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentLayerOverlayBinding.inflate(inflater, container, false)
        return binding.root
    }
}