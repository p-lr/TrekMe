package com.peterlaurence.trekme.features.map.presentation.viewmodel.controllers

import androidx.compose.runtime.mutableStateListOf
import com.peterlaurence.trekme.features.map.presentation.viewmodel.SnackBarEvent

class SnackBarController() {
    val snackBarEvents = mutableStateListOf<SnackBarEvent>()

    /**
     * Contract: When the view shows a snackbar, this method should be invoked.
     */
    fun onSnackBarShown() {
        snackBarEvents.removeFirstOrNull()
    }

    fun showSnackBar(type: SnackBarEvent) {
        snackBarEvents.add(type)
    }
}