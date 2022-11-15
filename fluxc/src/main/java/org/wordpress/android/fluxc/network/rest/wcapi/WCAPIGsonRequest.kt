package org.wordpress.android.fluxc.network.rest.wcapi

import com.android.volley.Response
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequest
import java.lang.reflect.Type

@Suppress("LongParameterList")
class WCAPIGsonRequest<T> : WPAPIGsonRequest<T> {
    constructor(method: Int, url: String?, params: Map<String, String>?, body: Map<String, Any>?,
                clazz: Class<T>?, listener: Response.Listener<T>?, errorListener: BaseErrorListener?) :
            super(method, url, params, body, clazz, listener, errorListener)

    constructor(method: Int, url: String?, params: Map<String, String>?, body: Map<String, Any>?,
                type: Type?, listener: Response.Listener<T>?, errorListener: BaseErrorListener?) :
            super(method, url, params, body, type, listener, errorListener)
}
