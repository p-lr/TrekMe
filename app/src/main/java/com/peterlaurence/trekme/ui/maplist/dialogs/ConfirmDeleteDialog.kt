package com.peterlaurence.trekme.ui.maplist.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.viewmodel.maplist.MapListViewModel

/**
 * The dialog that shows up when the user deletes a map.
 */
class ConfirmDeleteDialog : DialogFragment() {
    private val viewModel: MapListViewModel by activityViewModels()

    companion object {
        const val mapIdKey = "mapId"

        fun newInstance(mapId: Int): ConfirmDeleteDialog {
            val bundle = Bundle()
            bundle.putInt(mapIdKey, mapId)
            val f = ConfirmDeleteDialog()
            f.arguments = bundle
            return f
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setMessage(R.string.map_delete_question)
                .setPositiveButton(R.string.delete_dialog) { _, _ ->
                    /* Delete the map */
                    val mapId = arguments?.getInt(mapIdKey) ?: return@setPositiveButton
                    viewModel.deleteMap(mapId)
                }
                .setNegativeButton(R.string.cancel_dialog_string) { _, _ -> }
        return builder.create()
    }
}