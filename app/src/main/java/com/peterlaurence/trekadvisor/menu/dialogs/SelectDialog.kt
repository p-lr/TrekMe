package com.peterlaurence.trekadvisor.menu.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import com.peterlaurence.trekadvisor.R
import kotlinx.android.parcel.Parcelize
import org.greenrobot.eventbus.EventBus
import java.lang.Exception

class SelectDialog : DialogFragment() {
    private lateinit var title: String
    private lateinit var values: List<String>
    private lateinit var valueSelected: String
    private lateinit var selectDialogEvent: SelectDialogEvent

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_VALUES = "values"
        private const val ARG_VALUE_SELECTED = "valueSelected"
        private const val ARG_EVENT = "event"

        @JvmStatic
        fun newInstance(title: String, values: List<String>, valueSelected: String, event: SelectDialogEvent): SelectDialog {
            val fragment = SelectDialog()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putStringArrayList(ARG_VALUES, ArrayList(values))
            args.putString(ARG_VALUE_SELECTED, valueSelected)
            args.putParcelable(ARG_EVENT, event)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = arguments?.getString(ARG_TITLE) ?: ""
        values = arguments?.getStringArrayList(ARG_VALUES) ?: listOf()
        valueSelected = arguments?.getString(ARG_VALUE_SELECTED) ?: ""
        try {
            selectDialogEvent = arguments?.getParcelable(ARG_EVENT)!!
        } catch (e: Exception) {
            // bad luck
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity!!)
        builder.setTitle(title)
        val indexSelected = values.indexOf(valueSelected)
        builder.setSingleChoiceItems(values.toTypedArray(), indexSelected) { _: DialogInterface, i: Int ->
            selectDialogEvent.selection.clear()
            selectDialogEvent.selection.add(values[i])
        }
        builder.setPositiveButton(getText(R.string.ok_dialog)) { _: DialogInterface, _: Int ->
            if (this@SelectDialog::selectDialogEvent.isInitialized) {
                EventBus.getDefault().post(selectDialogEvent)
            }
        }
        builder.setNegativeButton(getText(R.string.cancel_dialog_string)) { _, _ -> dismiss() }
        return builder.create()
    }
}


/**
 * The event that must be subclassed by every events passed to [SelectDialog.newInstance].
 * This is to ensure that listeners for one type of [SelectDialogEvent] are not mistaken by other
 * type [SelectDialogEvent] that have a different purpose.
 * It is sent when the user presses the "OK" button of the dialog.
 */
@Parcelize
open class SelectDialogEvent(val selection: ArrayList<String>) : Parcelable