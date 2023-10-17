package org.wordpress.android.fluxc.network.rest.wpcom.wc.passkey

import android.content.Context
import com.android.volley.RequestQueue
import com.android.volley.Response
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import javax.inject.Inject

class PasskeyRestClient @Inject constructor(
    context: Context,
    dispatcher: Dispatcher,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
): BaseWPComRestClient(
    context,
    dispatcher,
    requestQueue,
    accessToken,
    userAgent
) {
    fun requestWebauthnChallenge(
        userId: Long,
        twoStepNonce: String
    ): WooPayload<WebauthnChallengeInfo> {
        val parameters = mapOf(
            "user_id" to userId.toString(),
            "two_step_nonce" to twoStepNonce,
            "auth_type" to "webauthn"
        )

        triggerAccountRequest(
            url = webauthnChallengeEndpointUrl,
            body = parameters,
            onSuccess = { WooPayload(it.asChallengeInfo) },
            onFailure = { WooPayload(it) }
        )

        return WooPayload(
            WebauthnChallengeInfo(
                challenge = "challenge",
                rpId = "rpId",
                twoStepNonce = "twoStepNonce",
                allowedCredentials = listOf("allowedCredentials")
            )
        )
    }

    private fun triggerAccountRequest(
        url: String,
        body: Map<String, Any>,
        onSuccess: (response: Map<String, Any>) -> Unit,
        onFailure: (error: WPComGsonNetworkError) -> Unit
    ) {
        val successListener = Response.Listener<Map<String, Any>> { onSuccess(it) }
        val failureListener = WPComErrorListener { onFailure(it) }

        val request = WPComGsonRequest.buildPostRequest(
            url,
            body,
            Map::class.java,
            successListener,
            failureListener
        )

        add(request)
    }

    private val Map<String, Any>.asChallengeInfo: WebauthnChallengeInfo
        get() = WebauthnChallengeInfo(
            challenge = this["challenge"] as String,
            rpId = this["rpId"] as String,
            twoStepNonce = this["twoStepNonce"] as String,
            allowedCredentials = this["allowedCredentials"]
                .run { this as? List<*> }
                ?.map { it as String }
        )


    companion object {
        private const val baseURLWithAction = "wp-login.php?action"
        private const val challengeEndpoint = "webauthn-challenge-endpoint"
        private const val authEndpoint = "webauthn-authentication-endpoint"
        const val webauthnChallengeEndpointUrl = "$baseURLWithAction=$challengeEndpoint"
    }
}