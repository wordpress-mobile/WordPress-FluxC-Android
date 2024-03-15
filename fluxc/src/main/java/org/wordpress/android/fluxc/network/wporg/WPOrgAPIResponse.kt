package org.wordpress.android.fluxc.network.wporg

import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError

sealed class WPOrgAPIResponse<T> {
    data class Success<T>(val data: T?) : WPOrgAPIResponse<T>()
    data class Error<T>(val error: BaseNetworkError) : WPOrgAPIResponse<T>()
}
