package com.stproject.client.android.core.common

import kotlinx.coroutines.CancellationException

fun Throwable.rethrowIfCancellation() {
    if (this is CancellationException) {
        throw this
    }
}
