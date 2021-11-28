package com.peterlaurence.trekme.features.map.presentation.viewmodel

import androidx.compose.runtime.mutableStateListOf

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