@file:Suppress("UNCHECKED_CAST", "RedundantVisibilityModifier", "NOTHING_TO_INLINE", "unused")
@file:OptIn(ExperimentalContracts::class)

package com.peterlaurence.trekme.util

import java.io.Serializable
import kotlin.contracts.*
import kotlin.jvm.JvmField
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmName

/**
 * A discriminated union that encapsulates a successful outcome with a value of type [T],
 * a loading, or a failure with an arbitrary [Throwable] exception.
 */
@SinceKotlin("1.3")
@JvmInline
public value class ResultL<out T> @PublishedApi internal constructor(
    @PublishedApi
    internal val value: Any?
) : Serializable {
    // discovery

    /**
     * Returns `true` if this instance represents a successful outcome.
     * In this case [isFailure] returns `false`.
     */
    public val isSuccess: Boolean get() = value !is Failure && value !is Loading

    public val isLoading: Boolean get() = value is Loading

    /**
     * Returns `true` if this instance represents a failed outcome.
     * In this case [isSuccess] returns `false`.
     */
    public val isFailure: Boolean get() = value is Failure

    // value & exception retrieval

    /**
     * Returns the encapsulated value if this instance represents [success][ResultL.isSuccess] or `null`
     * if it is [failure][ResultL.isFailure].
     *
     * This function is a shorthand for `getOrElse { null }` (see [getOrElse]) or
     * `fold(onSuccess = { it }, onFailure = { null })` (see [fold]).
     */
    public inline fun getOrNull(): T? =
        when {
            isFailure || isLoading -> null
            else -> value as T
        }

    /**
     * Returns the encapsulated [Throwable] exception if this instance represents [failure][isFailure] or `null`
     * if it is [success][isSuccess].
     *
     * This function is a shorthand for `fold(onSuccess = { null }, onFailure = { it })` (see [fold]).
     */
    public fun exceptionOrNull(): Throwable? =
        when (value) {
            is Failure -> value.exception
            else -> null
        }

    /**
     * Returns a string `Success(v)` if this instance represents [success][ResultL.isSuccess]
     * where `v` is a string representation of the value or a string `Failure(x)` if
     * it is [failure][isFailure] where `x` is a string representation of the exception.
     */
    public override fun toString(): String =
        when (value) {
            is Failure -> value.toString() // "Failure($exception)"
            is Loading -> "Loading"
            else -> "Success($value)"
        }

    // companion with constructors

    /**
     * Companion object for [ResultL] class that contains its constructor functions
     * [success] and [failure].
     */
    public companion object {
        /**
         * Returns an instance that encapsulates the given [value] as successful value.
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("success")
        public inline fun <T> success(value: T): ResultL<T> =
            ResultL(value)

        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("success")
        public inline fun <T> loading(): ResultL<T> =
            ResultL(createLoading())

        /**
         * Returns an instance that encapsulates the given [Throwable] [exception] as failure.
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("failure")
        public inline fun <T> failure(exception: Throwable): ResultL<T> =
            ResultL(createFailure(exception))
    }


    internal object Loading : Serializable

    internal class Failure(
        @JvmField
        val exception: Throwable
    ) : Serializable {
        override fun equals(other: Any?): Boolean = other is Failure && exception == other.exception
        override fun hashCode(): Int = exception.hashCode()
        override fun toString(): String = "Failure($exception)"
    }
}

/**
 * Creates an instance of internal marker [ResultL.Failure] class to
 * make sure that this class is not exposed in ABI.
 */
@PublishedApi
@SinceKotlin("1.3")
internal fun createFailure(exception: Throwable): Any =
    ResultL.Failure(exception)

/**
 * Creates an instance of internal marker [ResultL.Loading] class to
 * make sure that this class is not exposed in ABI.
 */
@PublishedApi
@SinceKotlin("1.3")
internal fun createLoading(): Any =
    ResultL.Loading

/**
 * Throws exception if the result is failure. This internal function minimizes
 * inlined bytecode for [getOrThrow] and makes sure that in the future we can
 * add some exception-augmenting logic here (if needed).
 */
@PublishedApi
@SinceKotlin("1.3")
internal fun ResultL<*>.throwOnFailure() {
    if (value is ResultL.Failure) throw value.exception
}

@PublishedApi
@SinceKotlin("1.3")
internal fun ResultL<*>.throwOnLoading() {
    if (value is ResultL.Loading) throw Exception("No value, loading")
}

public inline fun <T> Result<T>.asResultL(): ResultL<T> {
    return fold(
        onSuccess = { ResultL(it) },
        onFailure = {
            ResultL.failure(it)
        }
    )
}

public inline fun <T> ResultL<T>.asResult(): Result<T?> {
    return fold(
        onSuccess = { Result.success(it) },
        onLoading = { Result.success(null) },
        onFailure = { Result.failure(it)}
    )
}

// -- extensions ---

/**
 * Returns the encapsulated value if this instance represents [success][ResultL.isSuccess] or throws the encapsulated [Throwable] exception
 * if it is [failure][ResultL.isFailure].
 *
 * This function is a shorthand for `getOrElse { throw it }` (see [getOrElse]).
 */
@SinceKotlin("1.3")
public inline fun <T> ResultL<T>.getOrThrow(): T {
    throwOnFailure()
    throwOnLoading()
    return value as T
}

/**
 * Returns the encapsulated value if this instance represents [success][ResultL.isSuccess] or the
 * result of [onFailure] function for the encapsulated [Throwable] exception if it is [failure][ResultL.isFailure].
 *
 * Note, that this function rethrows any [Throwable] exception thrown by [onFailure] function.
 *
 * This function is a shorthand for `fold(onSuccess = { it }, onFailure = onFailure)` (see [fold]).
 */
@SinceKotlin("1.3")
public inline fun <R, T : R> ResultL<T>.getOrElse(onFailure: (exception: Throwable) -> R, onLoading: () -> R): R {
    contract {
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }

    return when (val exception = exceptionOrNull()) {
        null -> if (isSuccess) value as T else onLoading()
        else -> onFailure(exception)
    }
}

/**
 * Returns the encapsulated value if this instance represents [success][ResultL.isSuccess] or the
 * [defaultValue] if it is [failure][ResultL.isFailure] or if it's [loading][ResultL.Loading].
 *
 * This function is a shorthand for `getOrElse { defaultValue }` (see [getOrElse]).
 */
@SinceKotlin("1.3")
public inline fun <R, T : R> ResultL<T>.getOrDefault(defaultValue: R): R {
    if (isFailure || isLoading) return defaultValue
    return value as T
}

/**
 * Returns the result of [onSuccess] for the encapsulated value if this instance represents [success][ResultL.isSuccess]
 * or the result of [onLoading] function if it's [loading][ResultL.Loading]
 * or the result of [onFailure] function for the encapsulated [Throwable] exception if it is [failure][ResultL.isFailure].
 *
 * Note, that this function rethrows any [Throwable] exception thrown by [onSuccess] or by [onFailure] function.
 */
@SinceKotlin("1.3")
public inline fun <R, T> ResultL<T>.fold(
    onSuccess: (value: T) -> R,
    onLoading: () -> R,
    onFailure: (exception: Throwable) -> R
): R {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onLoading, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }
    return when (val exception = exceptionOrNull()) {
        null -> if (isSuccess) onSuccess(value as T) else onLoading()
        else -> onFailure(exception)
    }
}

// transformation

/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated value
 * if this instance represents [success][ResultL.isSuccess] or the
 * original encapsulated [Throwable] exception if it is [failure][ResultL.isFailure].
 *
 * Note, that this function rethrows any [Throwable] exception thrown by [transform] function.
 * See [mapCatching] for an alternative that encapsulates exceptions.
 */
@SinceKotlin("1.3")
public inline fun <R, T> ResultL<T>.map(transform: (value: T) -> R): ResultL<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when {
        isSuccess -> ResultL.success(transform(value as T))
        isLoading -> ResultL.loading()
        else -> ResultL(value)
    }
}

/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated value
 * if this instance represents [success][ResultL.isSuccess] or the
 * original encapsulated [Throwable] exception if it is [failure][ResultL.isFailure].
 *
 * This function catches any [Throwable] exception thrown by [transform] function and encapsulates it as a failure.
 * See [map] for an alternative that rethrows exceptions from `transform` function.
 */
@SinceKotlin("1.3")
public inline fun <R, T> ResultL<T>.mapCatching(transform: (value: T) -> R): ResultL<R> {
    return when {
        isSuccess -> runCatching { transform(value as T) }.asResultL()
        isLoading -> ResultL.loading()
        else -> ResultL(value)
    }
}

/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated [Throwable] exception
 * if this instance represents [failure][ResultL.isFailure] or the
 * original encapsulated value if it is [success][ResultL.isSuccess] or [loading][ResultL.isLoading].
 *
 * Note, that this function rethrows any [Throwable] exception thrown by [transform] function.
 * See [recoverCatching] for an alternative that encapsulates exceptions.
 */
@SinceKotlin("1.3")
public inline fun <R, T : R> ResultL<T>.recover(transform: (exception: Throwable) -> R): ResultL<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when (val exception = exceptionOrNull()) {
        null -> this
        else -> ResultL.success(transform(exception))
    }
}

/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated [Throwable] exception
 * if this instance represents [failure][ResultL.isFailure] or the
 * original encapsulated value if it is [success][ResultL.isSuccess].
 *
 * This function catches any [Throwable] exception thrown by [transform] function and encapsulates it as a failure.
 * See [recover] for an alternative that rethrows exceptions.
 */
@SinceKotlin("1.3")
public inline fun <R, T : R> ResultL<T>.recoverCatching(transform: (exception: Throwable) -> R): ResultL<R> {
    return when (val exception = exceptionOrNull()) {
        null -> this
        else -> runCatching { transform(exception) }.asResultL()
    }
}

// "peek" onto value/exception and pipe

/**
 * Performs the given [action] on the encapsulated [Throwable] exception if this instance represents [failure][ResultL.isFailure].
 * Returns the original `ResultL` unchanged.
 */
@SinceKotlin("1.3")
public inline fun <T> ResultL<T>.onFailure(action: (exception: Throwable) -> Unit): ResultL<T> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    exceptionOrNull()?.let { action(it) }
    return this
}

/**
 * Performs the given [action] on the encapsulated value if this instance represents [success][ResultL.isSuccess].
 * Returns the original `ResultL` unchanged.
 */
@SinceKotlin("1.3")
public inline fun <T> ResultL<T>.onSuccess(action: (value: T) -> Unit): ResultL<T> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    if (isSuccess) action(value as T)
    return this
}

/**
 * Performs the given [action] on the encapsulated value if this instance represents [loading][ResultL.isLoading].
 * Returns the original `ResultL` unchanged.
 */
@SinceKotlin("1.3")
public inline fun <T> ResultL<T>.onLoading(action: () -> Unit): ResultL<T> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    if (isLoading) action()
    return this
}

// -------------------
