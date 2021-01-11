package com.peterlaurence.trekme.ui.mapcreate.overlay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.databinding.FragmentLayerOverlayBinding
import com.peterlaurence.trekme.viewmodel.mapcreate.LayerOverlayViewModel

class LayerOverlayFragment : Fragment() {
    private var wmtsSource: WmtsSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wmtsSource = arguments?.let {
            LayerOverlayFragmentArgs.fromBundle(it)
        }?.wmtsSourceBundle?.wmtsSource
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentLayerOverlayBinding.inflate(inflater, container, false)

        val viewModel: LayerOverlayViewModel by viewModels()
        wmtsSource?.also { source ->
            viewModel.setSource(source)
        }

        return binding.root
    }
}