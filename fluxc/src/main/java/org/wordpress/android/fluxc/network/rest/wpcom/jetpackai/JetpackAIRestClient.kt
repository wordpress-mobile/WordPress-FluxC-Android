package org.wordpress.android.fluxc.network.rest.wpcom.jetpackai

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class JetpackAIRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchJetpackAICompletions(
        site: SiteModel,
        prompt: String
    ): JetpackAICompletionsResponse {
        val url = WPCOMV2.sites.site(site.siteId).jetpack_ai.completions.url
        val response = wpComGsonRequestBuilder.syncPostRequest(
            restClient = this,
            url = url,
            params = null,
            body = mapOf(
                "content" to prompt
            ),
            clazz = String::class.java
        )

        return when (response) {
            is Response.Success -> JetpackAICompletionsResponse.Success(response.data)
            is Response.Error -> JetpackAICompletionsResponse.Error(
                response.error.toJetpackAICompletionsError(),
                response.error.message
            )
        }
    }

    sealed class JetpackAICompletionsResponse {
        data class Success(val completion: String) : JetpackAICompletionsResponse()
        data class Error(
            val type: JetpackAICompletionsErrorType,
            val message: String? = null
        ) : JetpackAICompletionsResponse()
    }

    enum class JetpackAICompletionsErrorType {
        API_ERROR,
        AUTH_ERROR,
        GENERIC_ERROR,
        INVALID_RESPONSE,
        TIMEOUT,
        NETWORK_ERROR
    }

    private fun WPComGsonNetworkError.toJetpackAICompletionsError() =
        when (type) {
            GenericErrorType.TIMEOUT -> JetpackAICompletionsErrorType.TIMEOUT
            GenericErrorType.NO_CONNECTION,
            GenericErrorType.INVALID_SSL_CERTIFICATE,
            GenericErrorType.NETWORK_ERROR -> JetpackAICompletionsErrorType.NETWORK_ERROR
            GenericErrorType.SERVER_ERROR -> JetpackAICompletionsErrorType.API_ERROR
            GenericErrorType.PARSE_ERROR,
            GenericErrorType.NOT_FOUND,
            GenericErrorType.CENSORED,
            GenericErrorType.INVALID_RESPONSE -> JetpackAICompletionsErrorType.INVALID_RESPONSE
            GenericErrorType.HTTP_AUTH_ERROR,
            GenericErrorType.AUTHORIZATION_REQUIRED,
            GenericErrorType.NOT_AUTHENTICATED -> JetpackAICompletionsErrorType.AUTH_ERROR
            GenericErrorType.UNKNOWN -> JetpackAICompletionsErrorType.GENERIC_ERROR
            null -> JetpackAICompletionsErrorType.GENERIC_ERROR
        }
}

