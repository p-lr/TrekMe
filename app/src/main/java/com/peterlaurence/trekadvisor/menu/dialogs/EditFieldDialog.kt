package com.peterlaurence.trekadvisor.menu.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.WindowManager
import android.widget.EditText
import com.peterlaurence.trekadvisor.R
import com.peterlaurence.trekadvisor.menu.dialogs.EditFieldDialog.Companion.newInstance
import kotlinx.android.parcel.Parcelize
import org.greenrobot.eventbus.EventBus


/**
 * Generic Dialog to edit a field. It relies on Green Robots's [EventBus] to send back an event that
 * subclasses [EditFieldEvent].
 *
 * All required attributed are passed through the [newInstance] method.
 *
 * @author perterLaurence on 26/08/2018
 */
class EditFieldDialog : DialogFragment() {
    private lateinit var title: String
    private lateinit var initialValue: String
    private lateinit var editFieldEvent: EditFieldEvent

    private lateinit var editText: EditText

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_INIT_VALUE = "initValue"
        private const val ARG_EVENT = "event"

        @JvmStatic
        fun newInstance(title: String, initialValue: String, event: EditFieldEvent): EditFieldDialog {
            val fragment = EditFieldDialog()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putString(ARG_INIT_VALUE, initialValue)
            args.putParcelable(ARG_EVENT, event)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = arguments?.getString(ARG_TITLE) ?: ""
        initialValue = arguments?.getString(ARG_INIT_VALUE) ?: ""
        try {
            editFieldEvent = arguments?.getParcelable(ARG_EVENT)!!
        } catch (e: Exception) {
            // bad luck
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity!!)
        builder.setTitle(title)
        val view = activity!!.layoutInflater.inflate(R.layout.edit_field_dialog, null)

        editText = view.findViewById(R.id.edit_field_edittext)
        editText.setText(initialValue)
        editText.setSelection(initialValue.length)

        builder.setPositiveButton(getText(R.string.ok_dialog)) { dialogInterface: DialogInterface, i: Int ->
            if (this@EditFieldDialog::editFieldEvent.isInitialized) {
                editFieldEvent.initialValue = initialValue
                editFieldEvent.newValue = editText.text.toString()
                EventBus.getDefault().post(editFieldEvent)
            }
        }
        builder.setNegativeButton(getText(R.string.cancel_dialog_string)) { _, _ -> dismiss() }

        builder.setView(view)

        val dialog = builder.create()

        /* This is necessary for the soft keyboard to appear with the dialog */
        dialog.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        return dialog
    }
}

/**
 * The event that must be subclassed by every events passed to [EditFieldDialog.newInstance]
 * It is sent when the user presses the "OK" button of the dialog.
 */
@Parcelize
open class EditFieldEvent(var initialValue: String, var newValue: String) : Parcelable
