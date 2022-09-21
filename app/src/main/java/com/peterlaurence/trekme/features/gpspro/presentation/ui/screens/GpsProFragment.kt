package com.peterlaurence.trekme.features.gpspro.presentation.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.databinding.FragmentGpsProBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GpsProFragment : Fragment() {
    private var binding: FragmentGpsProBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        /* The action bar isn't managed by Compose */
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            show()
            title = getString(R.string.select_bt_devices_title)
        }

        binding = FragmentGpsProBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                /* Clear the existing action menu */
                menu.clear()

                /* Fill the new one */
                menuInflater.inflate(R.menu.menu_fragment_gpspro, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.help_gpspro_id -> {
                        val url = getString(R.string.gps_pro_help_url)
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(browserIntent)
                        true
                    }
                    else -> false
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}