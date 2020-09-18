package com.peterlaurence.trekme.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.databinding.FragmentAboutBinding

/**
 * Displays the link on the user manual, and encourages the user to give feedback about the
 * application.
 *
 * @author P.Laurence
 */
class AboutFragment : Fragment() {
    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.userManualBtn.setOnClickListener {
            val url = getString(R.string.help_url)
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        }

        /* In case of positive feedback, ask for app rating */
        binding.thumbUpButton.setOnClickListener {
            val packageName = requireContext().applicationContext.packageName
            val uri: Uri = Uri.parse("market://details?id=$packageName")
            val goToMarket = Intent(Intent.ACTION_VIEW, uri)
            try {
                startActivity(goToMarket)
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=$packageName")))
            }
        }

        /* In case of negative feedback, ask the user to send an email */
        binding.thumbDownButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                    .setMessage(getString(R.string.feedback_negative_msg))
                    .setPositiveButton(getString(R.string.ok_dialog)) { _, _ ->
                        val emailIntent = Intent(Intent.ACTION_SENDTO,
                                Uri.fromParts("mailto", getString(R.string.email_support), null))
                        startActivity(emailIntent)
                    }
                    .setNegativeButton(getString(R.string.cancel_dialog_string)) { d, _ ->
                        d.cancel()
                    }
                    .create().show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}