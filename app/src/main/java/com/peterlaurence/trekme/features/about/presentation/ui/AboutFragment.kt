package com.peterlaurence.trekme.features.about.presentation.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.databinding.FragmentAboutBinding
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme

/**
 * Displays the link on the user manual, and encourages the user to give feedback about the
 * application.
 */
class AboutFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        /* The action bar isn't managed by Compose */
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            show()
            title = getString(R.string.about)
        }

        val binding = FragmentAboutBinding.inflate(inflater, container, false)

        binding.aboutUi.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrekMeTheme {
                    AboutStateful(
                        onUserManual = this@AboutFragment::onUserManual,
                        onAppRating = this@AboutFragment::onAppRating,
                        onSendMail = this@AboutFragment::onSendMail
                    )
                }
            }
        }

        return binding.root
    }

    private fun onUserManual() {
        val url = getString(R.string.help_url)
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        runCatching {
            startActivity(browserIntent)
        }
    }

    private fun onAppRating() {
        val packageName = requireContext().applicationContext.packageName
        val uri: Uri = Uri.parse("market://details?id=$packageName")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                Uri.parse("http://play.google.com/store/apps/details?id=$packageName"))
            )
        }
    }

    private fun onSendMail() {
        val emailIntent = Intent(Intent.ACTION_SENDTO,
            Uri.fromParts("mailto", getString(R.string.email_support), null))
        runCatching {
            startActivity(emailIntent)
        }
    }
}