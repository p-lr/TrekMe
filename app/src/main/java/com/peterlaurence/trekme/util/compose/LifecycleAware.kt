package com.peterlaurence.trekme.util.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector

@Composable
fun <T> LaunchedEffectWithLifecycle(
    flow: Flow<T>,
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    collector: FlowCollector<T>
) {
    LaunchedEffect(key1 = lifecycle, key2 = flow) {
        lifecycle.repeatOnLifecycle(minActiveState) {
            flow.collect(collector)
        }
    }
}

@Composable
fun <T> LaunchedEffectWithLifecycle(
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    block: suspend () -> T
) {
    LaunchedEffect(key1 = lifecycle, key2 = block) {
        lifecycle.repeatOnLifecycle(minActiveState) {
            block()
        }
    }
}