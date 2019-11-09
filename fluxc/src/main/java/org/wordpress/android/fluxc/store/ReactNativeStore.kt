package org.wordpress.android.fluxc.store

import com.google.gson.JsonElement
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.reactnative.ReactNativeWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.reactnative.ReactNativeWPComRestClient
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Success
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Error as Error
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

abstract class Continuation<in T> : kotlin.coroutines.Continuation<T> {
    abstract fun resume(value: T)
    abstract fun resumeWithException(exception: Throwable)
    override fun resumeWith(result: Result<T>) = result.fold(::resume, ::resumeWithException)
}

@Singleton
class ReactNativeStore
@Inject constructor(
    private val wpComRestClient: ReactNativeWPComRestClient,
    private val wpAPIRestClient: ReactNativeWPAPIRestClient,
    private val coroutineContext: CoroutineContext
) {
    suspend fun performWPComRequest(url: String, params: Map<String, String>): ReactNativeFetchResponse =
            withContext(coroutineContext) {
                return@withContext wpComRestClient.fetch(url, params, ::Success, ::Error)
            }

    suspend fun performWPAPIRequest(url: String, params: Map<String, String>): ReactNativeFetchResponse =
            withContext(coroutineContext) {
                return@withContext wpAPIRestClient.fetch(url, params, ::Success, ::Error)
            }
}

sealed class ReactNativeFetchResponse {
    class Success(val result: JsonElement) : ReactNativeFetchResponse()
    class Error(networkError: BaseNetworkError) : ReactNativeFetchResponse() {
        val error = networkError.volleyError?.message
                ?: (networkError as? WPComGsonNetworkError)?.apiError
                ?: networkError.message
                ?: "Unknown ${networkError.javaClass.simpleName}"
    }
}
