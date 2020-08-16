package com.peterlaurence.trekme.ui.record.components.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.peterlaurence.trekme.R

/**
 * A dialog which informs the user why battery optimization can cause a GPX recording to be abruptly
 * stopped.
 * A solution is also (optionally) shown.
 *
 * @author P.Laurence on 16/08/20
 */
class BatteryOptWarningDialog : DialogFragment() {
    private var solutionShown = false
    private val solutionKey = "solution"

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_battery_opt_warning, null)
        solutionShown = savedInstanceState?.getBoolean(solutionKey) ?: false

        configureInit(view as ScrollView)

        return AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.warning_title))
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ok_dialog), null)
                .create()
    }

    private fun configureInit(view: ScrollView) {
        val warningText = view.findViewById<TextView>(R.id.battery_opt_warn_msg)
        val solutionBtn = view.findViewById<Button>(R.id.battery_warn_solution_btn)
        val solutionText = view.findViewById<TextView>(R.id.battery_warn_solution)
        val solutionTitle = view.findViewById<TextView>(R.id.battery_warn_solution_title)

        fun showSolution() {
            warningText.visibility = View.GONE
            solutionBtn.visibility = View.GONE
            solutionText.visibility = View.VISIBLE
            solutionTitle.visibility = View.VISIBLE
        }

        if (solutionShown) {
            showSolution()
            view.post {
                view.fullScroll(View.FOCUS_DOWN)
            }
        } else {
            solutionBtn.setOnClickListener {
                solutionShown = true
                showSolution()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(solutionKey, solutionShown)
    }
}