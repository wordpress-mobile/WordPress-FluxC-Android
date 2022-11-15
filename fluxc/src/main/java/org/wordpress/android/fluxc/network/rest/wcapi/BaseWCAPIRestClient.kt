package org.wordpress.android.fluxc.network.rest.wcapi

import com.android.volley.Request
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.BaseRequest.OnAuthFailedListener
import org.wordpress.android.fluxc.network.UserAgent

open class BaseWCAPIRestClient(private val mDispatcher: Dispatcher, private val mRequestQueue: RequestQueue,
                               private val mUserAgent: UserAgent) {

    private val mOnAuthFailedListener: OnAuthFailedListener =
            OnAuthFailedListener { authError ->
                mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateErrorAction(authError))
            }

    fun <T> add(request: WCAPIGsonRequest<T>): Request<T> {
        return mRequestQueue.add(setRequestAuthParams(request))
    }

    private fun <T> setRequestAuthParams(request: BaseRequest<T>): BaseRequest<T> {
        request.setOnAuthFailedListener(mOnAuthFailedListener)
        request.setUserAgent(mUserAgent.userAgent)
        return request
    }

}
