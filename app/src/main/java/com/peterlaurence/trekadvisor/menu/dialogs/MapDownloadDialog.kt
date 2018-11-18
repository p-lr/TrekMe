package com.peterlaurence.trekadvisor.menu.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.View
import com.peterlaurence.trekadvisor.R
import com.peterlaurence.trekadvisor.service.event.MapDownloadEvent
import com.peterlaurence.trekadvisor.service.event.Status
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