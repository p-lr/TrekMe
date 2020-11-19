package com.peterlaurence.trekme.ui.mapview.components.tracksmanage.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.databinding.DialogColorSelectBinding
import com.peterlaurence.trekme.ui.mapview.components.tracksmanage.events.TrackColorChangeEvent
import com.peterlaurence.trekme.ui.mapview.components.tracksmanage.views.SelectableColor
import com.peterlaurence.trekme.ui.mapview.events.MapViewEventBus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * A color picker for tracks. Displays 6 colors, along with the possibility to change between light,
 * normal, and dark palettes
 *
 * @author P.Laurence on 19/11/20
 */
@AndroidEntryPoint
class ColorSelectDialog : DialogFragment() {
    private var _binding: DialogColorSelectBinding? = null
    private val binding get() = _binding!!
    private var routeId: Int? = null
    private var selectedIndex = -1

    private val normalPalette = listOf("#F44336", "#9C27B0", "#2196F3", "#4CAF50",
            "#755548", "#607D8B")

    private val colorViews
        get() = listOf(binding.color1, binding.color2, binding.color3, binding.color4,
                binding.color5, binding.color6)

    @Inject
    lateinit var eventBus: MapViewEventBus

    companion object {
        private const val ROUTE_ID = "routeId"

        @JvmStatic
        fun newInstance(routeId: Int): ColorSelectDialog {
            val fragment = ColorSelectDialog()
            val args = Bundle()
            args.putInt(ROUTE_ID, routeId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogColorSelectBinding.inflate(LayoutInflater.from(context))

        routeId = arguments?.getInt(ROUTE_ID)
        colorViews.forEachIndexed { index: Int, selectableColor: SelectableColor ->
            selectableColor.setOnClickListener {
                selectableColor.isSelected = !selectableColor.isSelected
                selectedIndex = index
                deselectAllBut(selectableColor)
            }
        }

        return AlertDialog.Builder(requireActivity())
                .setTitle(R.string.color_picker_title)
                .setPositiveButton(R.string.apply_color_btn) { _, _ ->
                    if (selectedIndex != -1 && routeId != null) {
                        eventBus.postTrackColorChange(TrackColorChangeEvent(routeId!!, normalPalette[selectedIndex]))
                    }
                }
                .setView(binding.root).create()
    }

    private fun deselectAllBut(v: SelectableColor) {
        colorViews.filterNot { it == v }.forEach {
            it.isSelected = false
        }
    }

    override fun onStart() {
        super.onStart()
        setNormalPalette()
    }

    private fun setNormalPalette() {
        colorViews.forEachIndexed { index, selectableColor ->
            selectableColor.setColor(Color.parseColor(normalPalette[index]))
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}