package org.wordpress.android.fluxc.network.rest.wpcom.jetpackai

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
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
    suspend fun fetchJetpackAIJWTToken(
        site: SiteModel
    ) : JetpackAIJWTTokenResponse {
        val url = WPCOMV2.sites.site(site.siteId).jetpack_openai_query.jwt.url
        val response = wpComGsonRequestBuilder.syncPostRequest(
            restClient = this,
            url = url,
            params = null,
            body = null,
            clazz = JetpackAIJWTTokenDto::class.java,
        )

        return when (response) {
            is Response.Success -> JetpackAIJWTTokenResponse.Success(response.data.token)
            is Response.Error -> JetpackAIJWTTokenResponse.Error(
                response.error.toJetpackAICompletionsError(),
                response.error.message
            )
        }
    }

    suspend fun fetchJetpackAITextCompletion(
        prompt: String,
        token: String,
        feature: String
    ): JetpackAICompletionsResponse {
        val url = WPCOMV2.text_completion.url
        val body = mutableMapOf<String, Any>()
        body.apply {
            put("prompt", prompt)
            put("token", token)
            put("feature", feature)
        }

        val response = wpComGsonRequestBuilder.syncPostRequest(
            restClient = this,
            url = url,
            params = null,
            body = body,
            clazz = JetpackAITextCompletionDto::class.java
        )

        return when (response) {
            is Response.Success -> JetpackAICompletionsResponse.Success(response.data.completion)
            is Response.Error -> JetpackAICompletionsResponse.Error(
                response.error.toJetpackAICompletionsError(),
                response.error.message
            )
        }
    }

    /**
     * Fetches Jetpack AI completions for a given prompt.
     *
     * @param site      The site for which completions are fetched.
     * @param prompt    The prompt used to generate completions.
     * @param feature   Used by backend to track AI-generation usage and measure costs. Optional.
     * @param skipCache If true, bypasses the default 30-second throttle and fetches fresh data.
     * @param postId    Optional post ID to mark its content as generated by Jetpack AI. If provided,
     *                  a post meta`_jetpack_ai_calls` is added or updated, indicating the number
     *                  of times AI is used in the post. Not required if marking is not needed.
     */
    suspend fun fetchJetpackAICompletions(
        site: SiteModel,
        prompt: String,
        feature: String? = null,
        skipCache: Boolean = false,
        postId: Long? = null,
    ): JetpackAICompletionsResponse {
        val url = WPCOMV2.sites.site(site.siteId).jetpack_ai.completions.url
        val body = mutableMapOf<String, Any>()
        body.apply {
            put("content", prompt)
            postId?.let { put("post_id", it) }
            put("skip_cache", skipCache)
            feature?.let { put("feature", it) }
        }

        val response = wpComGsonRequestBuilder.syncPostRequest(
            restClient = this,
            url = url,
            params = null,
            body = body,
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

    internal  data class JetpackAIJWTTokenDto(
        @SerializedName ("success") val success: Boolean,
        @SerializedName("token") val token: String
    )

    internal data class JetpackAITextCompletionDto(
        @SerializedName ("completion") val completion: String
    )

    sealed class JetpackAIJWTTokenResponse {
        data class Success(val token: String) : JetpackAIJWTTokenResponse()
        data class Error(
            val type: JetpackAICompletionsErrorType,
            val message: String? = null
        ) : JetpackAIJWTTokenResponse()
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

