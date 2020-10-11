package com.peterlaurence.trekme.ui.record.components.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.ui.record.components.events.DiscardLocationDisclaimerEvent
import com.peterlaurence.trekme.ui.record.components.events.LocationDisclaimerClosedEvent
import org.greenrobot.eventbus.EventBus

/**
 * This disclaimer is shown after the user starts a GPX recording.
 * The user can opt-in for "never show this again". In this case, we send a
 * [DiscardLocationDisclaimerEvent].
 */
class LocalisationDisclaimer() : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.location_info_title))
                .setMessage(R.string.background_location_disclaimer)
                .setNeutralButton(R.string.understood_dialog) { _, _ ->
                    EventBus.getDefault().post(DiscardLocationDisclaimerEvent())
                    notifyClosed()
                }
                .setPositiveButton(getString(R.string.close_dialog)) { _, _ ->
                    notifyClosed()
                }
                .create()
    }

    private fun notifyClosed() {
        EventBus.getDefault().post(LocationDisclaimerClosedEvent())
    }

    override fun onCancel(dialog: DialogInterface) {
        notifyClosed()
        super.onCancel(dialog)
    }
}