package org.wordpress.android.fluxc.network.rest.wcapi

import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError

sealed class WCAPIResponse<T> {
    data class Success<T>(val data: T?) : WCAPIResponse<T>()
    data class Error<T>(val error: BaseNetworkError) : WCAPIResponse<T>()
}
