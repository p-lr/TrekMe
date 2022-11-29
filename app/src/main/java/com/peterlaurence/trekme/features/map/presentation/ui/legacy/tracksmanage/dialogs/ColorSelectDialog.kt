package com.peterlaurence.trekme.features.map.presentation.ui.legacy.tracksmanage.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.databinding.DialogColorSelectBinding
import com.peterlaurence.trekme.features.map.presentation.ui.legacy.tracksmanage.events.TrackColorChangeEvent
import com.peterlaurence.trekme.features.map.presentation.ui.legacy.tracksmanage.views.SelectableColor
import com.peterlaurence.trekme.features.map.presentation.ui.legacy.events.TracksEventBus
import com.peterlaurence.trekme.util.android.parcelable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
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
    private val paletteBundleKey = "palette"
    private val selectedBundleKey = "selected"
    private var paletteVariant = PaletteVariant.NORMAL
    private var routeId: String? = null
    private var selectedIndex = -1

    private val normalPalette = listOf("#f44336", "#9c27b0", "#2196f3", "#4caf50",
            "#755548", "#607d8b")

    private val lightPalette = listOf("#ff7961", "#d05ce3", "#6ec6ff", "#80e27e",
            "#a98274", "#8eacbb")

    private val darkPalette = listOf("#ba000d", "#6a0080", "#0069c0", "#087f23",
            "#4b2c20", "#34515e")

    private val colorViews
        get() = listOf(binding.color1, binding.color2, binding.color3, binding.color4,
                binding.color5, binding.color6)

    private val palette
        get() = when (paletteVariant) {
            PaletteVariant.NORMAL -> normalPalette
            PaletteVariant.LIGHT -> lightPalette
            PaletteVariant.DARK -> darkPalette
        }

    @Inject
    lateinit var eventBus: TracksEventBus

    companion object {
        private const val ROUTE_ID = "routeId"
        private const val COLOR_ID = "colorId"

        @JvmStatic
        fun newInstance(routeId: String, color: String?): ColorSelectDialog {
            val fragment = ColorSelectDialog()
            val args = Bundle()
            args.putString(ROUTE_ID, routeId)
            args.putString(COLOR_ID, color)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogColorSelectBinding.inflate(LayoutInflater.from(context))

        savedInstanceState?.parcelable<PaletteVariant>(paletteBundleKey)?.also {
            paletteVariant = it
        }

        selectedIndex = if (savedInstanceState != null) {
            savedInstanceState.getInt(selectedBundleKey, -1)
        } else {
            val currentColor = arguments?.getString(COLOR_ID)
            if (currentColor != null) {
                resolvePaletteAndIndex(currentColor)
            } else -1
        }

        routeId = arguments?.getString(ROUTE_ID)
        colorViews.forEachIndexed { index: Int, selectableColor: SelectableColor ->
            selectableColor.setOnClickListener {
                selectableColor.isSelected = !selectableColor.isSelected
                selectedIndex = index
                deselectAllBut(selectableColor)
            }
        }
        binding.variantNormalRadioBtn.setOnClickListener {
            setPalette(PaletteVariant.NORMAL)
        }
        binding.variantLightRadioBtn.setOnClickListener {
            setPalette(PaletteVariant.LIGHT)
        }
        binding.variantDarkRadioBtn.setOnClickListener {
            setPalette(PaletteVariant.DARK)
        }

        return AlertDialog.Builder(requireActivity())
                .setTitle(R.string.color_picker_title)
                .setPositiveButton(R.string.apply_color_btn) { _, _ ->
                    val routeId = routeId
                    if (selectedIndex != -1 && routeId != null) {
                        eventBus.postTrackColorChange(TrackColorChangeEvent(routeId, palette[selectedIndex]))
                    }
                }
                .setNegativeButton(R.string.cancel_dialog_string) { _, _ -> }
                .setView(binding.root).create()
    }

    private fun deselectAllBut(v: SelectableColor) {
        colorViews.filterNot { it == v }.forEach {
            it.isSelected = false
        }
    }

    override fun onStart() {
        super.onStart()
        setPalette(paletteVariant)

        /* Restore previous selection */
        if (selectedIndex != -1) {
            colorViews.getOrNull(selectedIndex)?.apply {
                isSelected = true
            }
        }
    }

    private fun setPalette(variant: PaletteVariant) {
        paletteVariant = variant
        colorViews.forEachIndexed { index, selectableColor ->
            selectableColor.setColor(Color.parseColor(palette[index]))
        }
    }

    /**
     * Returns the index of the the color in the palette where it was found, or -1.
     * Also set internal state such as [paletteVariant] and synchronizes the radio buttons.
     */
    private fun resolvePaletteAndIndex(currentColor: String): Int {
        val palettes = listOf(normalPalette, lightPalette, darkPalette)
        var index = -1
        val paletteIndex = palettes.indexOfFirst {
            index = it.indexOf(currentColor)
            index != -1
        }
        when (paletteIndex) {
            0 -> {
                binding.variantNormalRadioBtn.isChecked = true
                paletteVariant = PaletteVariant.NORMAL
            }
            1 -> {
                binding.variantLightRadioBtn.isChecked = true
                paletteVariant = PaletteVariant.LIGHT
            }
            2 -> {
                binding.variantDarkRadioBtn.isChecked = true
                paletteVariant = PaletteVariant.DARK
            }
        }

        return index
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(paletteBundleKey, paletteVariant)
        outState.putInt(selectedBundleKey, selectedIndex)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

@Parcelize
private enum class PaletteVariant : Parcelable {
    NORMAL, LIGHT, DARK
}