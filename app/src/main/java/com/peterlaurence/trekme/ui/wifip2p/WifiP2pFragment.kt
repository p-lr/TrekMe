package com.peterlaurence.trekme.ui.wifip2p

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.wifip2p.*
import com.peterlaurence.trekme.databinding.FragmentWifip2pBinding
import com.peterlaurence.trekme.ui.dialogs.MapChoiceDialog
import com.peterlaurence.trekme.ui.dialogs.MapSelectedEvent
import com.peterlaurence.trekme.viewmodel.wifip2p.Errors
import com.peterlaurence.trekme.viewmodel.wifip2p.ServiceAlreadyStarted
import com.peterlaurence.trekme.viewmodel.wifip2p.WifiP2pViewModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

/**
 * The frontend for the [WifiP2pService].
 * It reacts to some of the [WifiP2pState]s emitted by the service.
 *
 * @author P.Laurence on 07/04/20
 */
class WifiP2pFragment : Fragment() {
    private var _binding: FragmentWifip2pBinding? = null
    private val binding get() = _binding!!

    @Suppress("UNCHECKED_CAST")
    private val viewModel: WifiP2pViewModel by viewModels(factoryProducer = {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return if (modelClass.isAssignableFrom(WifiP2pViewModel::class.java)) {
                    WifiP2pViewModel(requireActivity().application) as T
                } else throw Exception()
            }
        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.state.observe(this, Observer {
            it?.let { state ->
                onState(state)
            }
        })

        viewModel.errors.observe(this, Observer {
            it?.let { onError(it) }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentWifip2pBinding.inflate(inflater, container, false)

        binding.receiveBtn.setOnClickListener {
            viewModel.onRequestReceive()
            binding.sendBtn.isEnabled = false
        }

        binding.sendBtn.setOnClickListener {
            val dialog = MapChoiceDialog()
            dialog.show(requireActivity().supportFragmentManager, "MapChoiceDialog")
            binding.receiveBtn.isEnabled = false
        }

        binding.stopBtn.setOnClickListener {
            viewModel.onRequestStop()
        }
        binding.stopBtn.isVisible = false

        return binding.root
    }

    @Subscribe
    fun onMapSelected(event: MapSelectedEvent) {
        viewModel.onRequestSend(event.mapId)
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
            AwaitingP2pConnection -> binding.status.text = getString(R.string.wifip2p_device_found)
            P2pConnected -> binding.status.text = getString(R.string.wifip2p_connected)
            is Loading -> onLoading(state.progress)
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
                        binding.stoppedStatus.text = getString(R.string.wifip2p_error)
                    }
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
            ServiceAlreadyStarted -> binding.status.editableText.append("Service already started")
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
        checkWifiState(requireContext())
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
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
}