package com.peterlaurence.trekme.features.maplist.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.databinding.FragmentMapListBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Shows the list of maps.
 */
@AndroidEntryPoint
class MapListFragment : Fragment() {
    private var binding: FragmentMapListBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        /* The action bar isn't managed by Compose */
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            show()
            title = getString(R.string.app_name)
        }

        binding = FragmentMapListBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}