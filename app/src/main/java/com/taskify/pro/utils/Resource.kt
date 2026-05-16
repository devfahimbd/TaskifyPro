package com.taskify.pro.utils

/**
 * Generic wrapper that represents the state of a single async operation.
 *
 * Using a sealed class guarantees exhaustive `when` coverage and makes
 * it impossible to accidentally access a value that isn't ready yet.
 */
sealed class Resource<out T> {

    /** The operation is still in progress. */
    data class Loading<out T>(val data: T? = null) : Resource<T>()

    /** The operation finished successfully. */
    data class Success<out T>(val data: T) : Resource<T>()

    /** The operation failed with an exception message. */
    data class Error(val message: String) : Resource<Nothing>()

    val isLoading: Boolean get() = this is Loading

    val isSuccessful: Boolean get() = this is Success
}
