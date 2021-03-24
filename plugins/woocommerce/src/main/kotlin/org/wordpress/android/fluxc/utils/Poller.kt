package org.wordpress.android.fluxc.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload

class Poller(private val delayInMs: Long, private val maxRetries: Int) {
    suspend fun <T : WooPayload<*>> poll(
        request: suspend () -> T,
        predicate: (T) -> Boolean,
    ): T {
        return pollInternal(request)
                .filter { predicate.invoke(it) }
                .first()
    }

    private suspend fun <T : WooPayload<*>> pollInternal(
        request: suspend () -> T,
    ): Flow<T> {
        var retries = 0
        return flow {
            while (retries < maxRetries) {
                val result = request.invoke()
                if (result.isError) {
                    retries++
                    if (retries == maxRetries) {
                        emit(result)
                    }
                } else {
                    retries = 0
                    emit(result)
                }
                delay(delayInMs)
            }
        }
    }
}
