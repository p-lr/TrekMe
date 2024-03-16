package com.peterlaurence.trekme.main.eventhandler

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.peterlaurence.trekme.events.GenericMessage
import com.peterlaurence.trekme.events.StandardMessage
import com.peterlaurence.trekme.events.WarningMessage
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import com.peterlaurence.trekme.util.compose.showSnackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun HandleGenericMessages(
    genericMessages: Flow<GenericMessage>,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onShowWarningDialog: (WarningMessage) -> Unit
) {
    LaunchedEffectWithLifecycle(genericMessages) { message ->
        when (message) {
            is StandardMessage -> {
                scope.launch {
                    snackbarHostState.showSnackbar(message = message.msg, isLong = message.showLong)
                }
            }

            is WarningMessage -> {
                onShowWarningDialog(message)
            }
        }
    }
}