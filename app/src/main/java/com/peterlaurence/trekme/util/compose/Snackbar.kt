package com.peterlaurence.trekme.util.compose

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

suspend fun SnackbarHostState.showSnackbar(
    message: String,
    isLong: Boolean = false,
    actionLabel: String? = null,
): SnackbarResult {
    return showSnackbar(
        message,
        actionLabel = actionLabel,
        duration = if (isLong) SnackbarDuration.Long else SnackbarDuration.Short
    )
}