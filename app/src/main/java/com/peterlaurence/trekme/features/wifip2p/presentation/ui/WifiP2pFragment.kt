package com.peterlaurence.trekme.features.wifip2p.presentation.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.databinding.FragmentWifip2pBinding
import com.peterlaurence.trekme.features.wifip2p.domain.service.*
import com.peterlaurence.trekme.features.wifip2p.presentation.ui.dialogs.MapSelectionForSend
import com.peterlaurence.trekme.features.wifip2p.presentation.events.WifiP2pEventBus
import com.peterlaurence.trekme.features.wifip2p.presentation.viewmodel.Errors
import com.peterlaurence.trekme.features.wifip2p.presentation.viewmodel.ServiceAlreadyStarted
import com.peterlaurence.trekme.features.wifip2p.presentation.viewmodel.WifiP2pViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The frontend for the [WifiP2pService].
 * It reacts to some of the [WifiP2pState]s emitted by the service.
 *
 * @author P.Laurence on 07/04/20
 */
@AndroidEntryPoint
class WifiP2pFragment : Fragment() {
    private var _binding: FragmentWifip2pBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var eventBus: WifiP2pEventBus

    private val viewModel: WifiP2pViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.state.observe(this) {
            it?.let { state ->
                onState(state)
            }
        }

        viewModel.errors.observe(this) {
            it?.let { onError(it) }
        }

        lifecycleScope.launch {
            eventBus.mapSelectedEvent.collect {
                viewModel.onRequestSend(it)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        /* The action bar isn't managed by Compose */
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            show()
            title = getString(R.string.wifip2p_title)
        }

        _binding = FragmentWifip2pBinding.inflate(inflater, container, false)

        binding.receiveBtn.setOnClickListener {
            viewModel.onRequestReceive()
            binding.sendBtn.isEnabled = false
        }

        binding.sendBtn.setOnClickListener {
            val dialog = MapSelectionForSend()
            dialog.show(requireActivity().supportFragmentManager, "MapSelectionForSend")
            binding.receiveBtn.isEnabled = false
        }

        binding.stopBtn.setOnClickListener {
            viewModel.onRequestStop()
        }
        binding.stopBtn.isVisible = false

        return binding.root
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
                menuInflater.inflate(R.menu.menu_fragment_wifip2p, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.help_wifip2p_id -> {
                        val url = getString(R.string.wifip2p_help_url)
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(browserIntent)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun onState(state: WifiP2pState) {
        binding.receiveBtn.isEnabled = false
        binding.sendBtn.isEnabled = false

        if (state > Started) {
            binding.searchView.visibility = View.GONE
            binding.waveSearchIndicator.stop()
            binding.stopBtn.isVisible = true
        }

        when (state) {
            Started -> onStarted()
            AwaitingSocketConnection, SocketConnected -> {
            } // Don't display anything for now
            AwaitingP2pConnection -> binding.status.text = getString(R.string.wifip2p_device_found)
            P2pConnected -> binding.status.text = getString(R.string.wifip2p_connected)
            is Loading -> onLoading(state.progress)
            Stopping -> {
            } // Don't display anything for now
            is Stopped -> {
                binding.receiveBtn.isEnabled = true
                binding.sendBtn.isEnabled = true
                binding.stopBtn.visibility = View.GONE
                binding.uploadView.visibility = View.GONE
                binding.status.text = ""

                when (state.stopReason) {
                    is MapSuccessfullyLoaded -> {
                        binding.stoppedView.visibility = View.VISIBLE
                        binding.emojiDisappointedFace.visibility = View.GONE
                        binding.emojiPartyFace.visibility = View.VISIBLE
                        binding.stoppedStatus.text = getString(R.string.wifip2p_successful_load).format(state.stopReason.name)
                    }
                    is WithError -> {
                        binding.stoppedView.visibility = View.VISIBLE
                        binding.emojiDisappointedFace.visibility = View.VISIBLE
                        binding.emojiPartyFace.visibility = View.GONE
                        binding.stoppedStatus.text = getString(R.string.wifip2p_error).format(state.stopReason.error.name)
                    }
                    is ByUser, null -> {
                    } // Don't display anything for now
                }

                /* Check Wifi state again */
                checkWifiState(requireContext())
            }
        }
    }

    private fun onStarted() {
        binding.status.text = getString(R.string.wifip2p_searching)
        binding.waveSearchIndicator.start()
        binding.uploadView.visibility = View.GONE
        binding.stoppedView.visibility = View.GONE
        binding.stopBtn.visibility = View.VISIBLE
        binding.searchView.visibility = View.VISIBLE
        binding.warningView.visibility = View.GONE
    }

    private fun onLoading(percent: Int) {
        binding.status.text = getString(R.string.wifip2p_loading)
        binding.uploadView.visibility = View.VISIBLE
        binding.stoppedView.visibility = View.GONE
        binding.progressBar.progress = percent
        binding.stopBtn.visibility = View.VISIBLE
    }

    private fun onError(error: Errors) {
        when (error) {
            ServiceAlreadyStarted -> _binding?.status?.editableText?.append("Service already started")
        }
    }

    override fun onStart() {
        super.onStart()
        checkWifiState(requireContext())
    }

    private fun checkWifiState(context: Context) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager != null) {
            if (!wifiManager.isWifiEnabled) {
                binding.warningView.visibility = View.VISIBLE
                binding.warningMessage.text = getString(R.string.wifip2p_warning_wifi)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}