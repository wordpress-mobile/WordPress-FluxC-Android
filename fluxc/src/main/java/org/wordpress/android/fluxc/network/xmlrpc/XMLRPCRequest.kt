package org.wordpress.android.fluxc.network.xmlrpc

import android.util.Xml
import com.android.volley.AuthFailureError
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Response
import com.android.volley.Response.Listener
import com.android.volley.VolleyError
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.HTTP_AUTH_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_SSL_CERTIFICATE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_AUTHENTICATED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest.XmlRpcErrorType.AUTH_REQUIRED
import org.wordpress.android.fluxc.store.AccountStore.AuthenticateErrorPayload
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.utils.ErrorUtils.OnUnexpectedError
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.API
import org.xmlpull.v1.XmlPullParserException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

// TODO: Would be great to use generics / return POJO or model direclty (see GSON code?)
open class XMLRPCRequest(
    url: String?,
    private val mMethod: XMLRPC,
    params: List<Any?>?,
    private val mListener: Listener<in Array<Any>?>,
    errorListener: BaseErrorListener?
) : BaseRequest<Any?>(Method.POST, url!!, errorListener) {
    private val mParams = params?.toTypedArray()
    private val mSerializer = Xml.newSerializer()

    enum class XmlRpcErrorType {
        NOT_SET, METHOD_NOT_ALLOWED, UNABLE_TO_READ_SITE, AUTH_REQUIRED
    }

    protected override fun deliverResponse(response: Any?) {
        deliverResponse(mListener, response)
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<Any?>? {
        return try {
            val data = String(response.data, Charset.defaultCharset())
            val `is`: InputStream = ByteArrayInputStream(data.toByteArray(Charset.forName("UTF-8")))
            val obj = XMLSerializerUtils.deserialize(XMLSerializerUtils.scrubXmlResponse(`is`))
            Response.success(obj, createCacheEntry(response))
        } catch (e: XMLRPCFault) {
            Response.error(VolleyError(e))
        } catch (e: UnsupportedEncodingException) {
            Response.error(ParseError(e))
        } catch (e: IOException) {
            AppLog.e(API, "Can't deserialize XMLRPC response", e)
            Response.error(ParseError(e))
        } catch (e: XmlPullParserException) {
            AppLog.e(API, "Can't deserialize XMLRPC response", e)
            Response.error(ParseError(e))
        } catch (e: XMLRPCException) {
            AppLog.e(API, "Can't deserialize XMLRPC response", e)
            Response.error(ParseError(e))
        }
    }

    override fun getBodyContentType(): String {
        return PROTOCOL_CONTENT_TYPE
    }

    @Throws(AuthFailureError::class) override fun getBody(): ByteArray? {
        try {
            val stringWriter = XMLSerializerUtils.serialize(mSerializer, mMethod, mParams)
            return stringWriter.toString().toByteArray(charset("UTF-8"))
        } catch (e: UnsupportedEncodingException) {
            AppLog.e(API, "Can't encode XMLRPC request", e)
        } catch (e: IOException) {
            AppLog.e(API, "Can't serialize XMLRPC request", e)
        }
        return null
    }

    override fun deliverBaseNetworkError(error: BaseNetworkError): BaseNetworkError {
        val payload = AuthenticateErrorPayload(GENERIC_ERROR)
        // XMLRPC errors are not managed in the layer below (BaseRequest), so check them here:
        if (error.hasVolleyError() && error.volleyError.cause is XMLRPCFault) {
            val xmlrpcFault = error.volleyError.cause as XMLRPCFault?
            if (xmlrpcFault!!.faultCode == 401) {
                error.type = AUTHORIZATION_REQUIRED // Augmented error
                payload.error.type = AuthenticationErrorType.AUTHORIZATION_REQUIRED
            } else if (xmlrpcFault.faultCode == 403) {
                error.type = NOT_AUTHENTICATED // Augmented error
                payload.error.type = AuthenticationErrorType.NOT_AUTHENTICATED
            } else if (xmlrpcFault.faultCode == 404) {
                error.type = NOT_FOUND // Augmented error
            }
            error.message = xmlrpcFault.message
        }
        when (error.type) {
            HTTP_AUTH_ERROR -> {
                payload.error.type = AuthenticationErrorType.HTTP_AUTH_ERROR
                payload.error.xmlRpcErrorType = AUTH_REQUIRED
            }
            INVALID_SSL_CERTIFICATE -> payload.error.type = AuthenticationErrorType.INVALID_SSL_CERTIFICATE
            else -> {
            }
        }
        if (payload.error.type != GENERIC_ERROR) {
            mOnAuthFailedListener.onAuthFailed(payload)
        }
        return error
    }

    /**
     * Helper method to capture the Listener's wildcard parameter type and use it to cast the response before
     * calling `onResponse()`.
     */
    private fun <K> deliverResponse(listener: Listener<K?>, rawResponse: Any?) {
        // The XMLRPCSerializer always returns an Object - it's up to the client making the request to know whether
        // it's really an Object[] (i.e., when requesting a list of values from the API).
        // We've already restricted the Listener parameterization to Object and Object[], so we know this is returning
        // a 'safe' type - but it's still up to the client to know if an Object or an Object[] is the expected response.
        // So, we're matching the parsed response to the Listener parameter we were given, trusting that the network
        // client knows what it's doing
        val response = rawResponse as K
        try {
            listener.onResponse(response)
        } catch (e: ClassCastException) {
            // If we aren't returning the type the client was expecting, treat this as an API response parse error
            val onUnexpectedError = OnUnexpectedError(
                e,
                "API response parse error: " + e.message
            )
            onUnexpectedError.addExtra(OnUnexpectedError.KEY_URL, url)
            onUnexpectedError.addExtra(OnUnexpectedError.KEY_RESPONSE, response.toString())
            mOnParseErrorListener.onParseError(onUnexpectedError)
            listener.onResponse(null)
        }
    }

    companion object {
        private const val PROTOCOL_CHARSET = "utf-8"
        private val PROTOCOL_CONTENT_TYPE = String.format("text/xml; charset=%s", PROTOCOL_CHARSET)
    }
}
