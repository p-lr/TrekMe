package com.peterlaurence.trekme.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.view.View
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.service.event.MapDownloadEvent
import com.peterlaurence.trekme.service.event.Status
import kotlinx.android.synthetic.main.download_map_dialog.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

/**
 * Dialog that shows the progression of a download.
 *
 * @author perterLaurence on 30/06/2018
 */
class MapDownloadDialog : DialogFragment() {

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe
    fun onProgressEvent(event: MapDownloadEvent) {
        when (event.status) {
            Status.PENDING -> progressBar.progress = event.progress.toInt()
            Status.FINISHED -> {
                dismiss()
            }
            Status.IMPORT_ERROR -> {
                errorMsg.visibility = View.VISIBLE
            }
            else -> dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity!!)
        builder.setTitle(getString(R.string.map_download_dialog_title))
        val view = activity!!.layoutInflater.inflate(R.layout.download_map_dialog, null)
        builder.setView(view)

        return builder.create()
    }
}