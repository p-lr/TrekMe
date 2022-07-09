package com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.record.presentation.events.RecordEventBus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * This disclaimer is shown after the user starts a GPX recording.
 * The user can opt-in for "never show this again". In this case, we discard the disclaimer.
 */
@AndroidEntryPoint
class LocalisationDisclaimer : DialogFragment() {
    @Inject
    lateinit var eventBus: RecordEventBus

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.location_info_title))
                .setMessage(R.string.background_location_disclaimer)
                .setNeutralButton(R.string.understood_dialog) { _, _ ->
                    eventBus.discardLocationDisclaimer()
                    notifyClosed()
                }
                .setPositiveButton(getString(R.string.close_dialog)) { _, _ ->
                    notifyClosed()
                }
                .create()
    }

    private fun notifyClosed() {
        eventBus.closeLocationDisclaimer()
    }

    override fun onCancel(dialog: DialogInterface) {
        notifyClosed()
        super.onCancel(dialog)
    }
}