package com.peterlaurence.trekme.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Limit the rate at which a [block] is called.
 * The [block] is invoked using the last element of type [T] sent to the returned channel.
 * This invocation is performed every [wait] ms.
 *
 * @param wait The time in ms between each [block] call.
 * @param block The actual block of code to execute.
 * @return a [SendChannel] to which elements are sent.
 *
 * @author P.Laurence on 23/08/2020
 */
fun <T> CoroutineScope.throttle(wait: Long, block: (T) -> Unit): SendChannel<T> {

    val channel = Channel<T>(capacity = Channel.CONFLATED)
    val flow = channel.receiveAsFlow()
    launch {
        flow.collect { elem ->
            block(elem)
            delay(wait)
        }
    }
    return channel
}