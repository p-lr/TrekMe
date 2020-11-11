package com.peterlaurence.trekme.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.peterlaurence.trekme.R

/**
 * An abstract dialog for selecting a single value.
 *
 * @author P.Laurence on 02/11/2018
 */
abstract class SingleSelectDialog : DialogFragment() {
    private lateinit var title: String
    private lateinit var values: List<String>
    private lateinit var valueSelected: String

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_VALUES = "values"
        const val ARG_VALUE_SELECTED = "valueSelected"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = arguments?.getString(ARG_TITLE) ?: ""
        values = arguments?.getStringArrayList(ARG_VALUES) ?: listOf()
        valueSelected = arguments?.getString(ARG_VALUE_SELECTED) ?: ""
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(title)
        val indexSelected = values.indexOf(valueSelected)
        var selection: String? = null
        builder.setSingleChoiceItems(values.toTypedArray(), indexSelected) { _: DialogInterface, i: Int ->
            selection = values[i]
        }
        builder.setPositiveButton(getText(R.string.ok_dialog)) { _: DialogInterface, _: Int ->
            selection?.also {
                onSelection(it)
            }
        }
        builder.setNegativeButton(getText(R.string.cancel_dialog_string)) { _, _ -> dismiss() }
        return builder.create()
    }

    abstract fun onSelection(selection: String)
}
