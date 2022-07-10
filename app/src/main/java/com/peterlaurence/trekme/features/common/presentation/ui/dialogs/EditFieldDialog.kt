package com.peterlaurence.trekme.features.common.presentation.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.peterlaurence.trekme.R


/**
 * Generic Dialog to edit a field. Subclasses should define what to do upon validation of a change.
 *
 * Title and initial value should passed through the [newInstance] method in the subclassed
 * implementation.
 *
 * @author perterLaurence on 26/08/2018
 */
abstract class EditFieldDialog : DialogFragment() {
    private lateinit var title: String
    private lateinit var initialValue: String

    private lateinit var editText: EditText

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_INIT_VALUE = "initValue"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = arguments?.getString(ARG_TITLE) ?: ""
        initialValue = arguments?.getString(ARG_INIT_VALUE) ?: ""
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(title)
        val view = requireActivity().layoutInflater.inflate(R.layout.edit_field_dialog, null)

        editText = view.findViewById(R.id.edit_field_edittext)
        editText.setText(initialValue)
        editText.setSelection(initialValue.length)

        builder.setPositiveButton(getText(R.string.ok_dialog)) { _: DialogInterface, _: Int ->
            onEditField(initialValue, editText.text.toString())
        }
        builder.setNegativeButton(getText(R.string.cancel_dialog_string)) { _, _ -> dismiss() }

        builder.setView(view)

        val dialog = builder.create()

        /* This is necessary for the soft keyboard to appear with the dialog */
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        return dialog
    }

    abstract fun onEditField(initialValue: String, newValue: String)
}
