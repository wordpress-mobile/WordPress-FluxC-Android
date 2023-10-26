package org.wordpress.android.fluxc.network.rest.wpcom.auth.passkey

import android.content.Context
import android.util.Base64
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.google.gson.Gson
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.suspendCoroutine

@Singleton
class PasskeyRestClient @Inject constructor(
    context: Context,
    dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(
    context,
    dispatcher,
    requestQueue,
    accessToken,
    userAgent
) {
    @Suppress("LongParameterList")
    suspend fun authenticateWebauthnSignature(
        userId: Long,
        clientId: Long,
        secret: String,
        twoStepNonce: String,
        credentialId: ByteArray,
        clientDataJson: ByteArray,
        authenticatorData: ByteArray,
        signature: ByteArray,
        userHandle: ByteArray
    ): String {
        val clientData = mapOf(
            "id" to Base64.encode(credentialId, Base64.DEFAULT),
            "rawId" to Base64.encode(credentialId, Base64.DEFAULT),
            "type" to "public-key",
            "clientExtensionResults" to mapOf<String, Any>(),
            "response" to mapOf(
                "clientDataJSON" to Base64.encode(clientDataJson, Base64.DEFAULT),
                "authenticatorData" to Base64.encode(authenticatorData, Base64.DEFAULT),
                "signature" to Base64.encode(signature, Base64.DEFAULT),
                "userHandle" to Base64.encode(userHandle, Base64.DEFAULT)
            )
        ).let { Gson().toJson(it) }

        val parameters = mapOf(
            "user_id" to userId.toString(),
            "client_id" to clientId.toString(),
            "client_secret" to secret,
            "auth_type" to "webauthn",
            "two_step_nonce" to twoStepNonce,
            "client_data" to clientData,
            "get_bearer_token" to true.toString(),
            "create_2fa_cookies_only" to true.toString()
        )

        return suspendCoroutine { cont ->
            triggerAccountRequest(
                url = webauthnAuthEndpointUrl,
                parameters = parameters,
                onSuccess = {
                    cont.resumeWith(Result.success(it.asBearerToken))
                },
                onFailure = {
                    val exception = Exception(it.message)
                    cont.resumeWith(Result.failure(exception))
                }
            )
        }
    }

    private fun triggerAccountRequest(
        url: String,
        parameters: Map<String, String>,
        onSuccess: (response: Map<*, *>) -> Unit,
        onFailure: (error: WPComGsonNetworkError) -> Unit
    ) {
        val successListener = Response.Listener<Map<*, *>> { onSuccess(it) }
        val failureListener = WPComErrorListener { onFailure(it) }

        val request = WPComGsonRequest.buildPostRequest(
            url,
            parameters,
            emptyMap(),
            Map::class.java,
            successListener,
            failureListener
        )

        add(request)
    }

    private val Map<*, *>.asBearerToken: String
        get() = this["data"]
            ?.run { this as? Map<*, *> }
            ?.let { this["bearer_token"] as? String }
            .orEmpty()

    companion object {
        private const val baseWPLoginUrl = "https://wordpress.com/wp-login.php?action"
        private const val challengeEndpoint = "webauthn-challenge-endpoint"
        private const val authEndpoint = "webauthn-authentication-endpoint"
        const val webauthnChallengeEndpointUrl = "$baseWPLoginUrl=$challengeEndpoint"
        const val webauthnAuthEndpointUrl = "$baseWPLoginUrl=$authEndpoint"
    }
}