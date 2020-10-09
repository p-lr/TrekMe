package com.peterlaurence.trekme.ui.record.components.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.settings.Settings

/**
 * This disclaimer is shown after the user starts a GPX recording.
 * The user can opt-in for "never show this again".
 */
class LocalisationDisclaimer(private val settings: Settings) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.location_info_title))
                .setMessage(R.string.background_location_disclaimer)
                .setNeutralButton(R.string.understood_dialog) { _, _ ->
                    settings.discardLocationDisclaimer()
                    dismiss()
                }
                .setPositiveButton(getString(R.string.close_dialog), null)
                .create()
    }
}