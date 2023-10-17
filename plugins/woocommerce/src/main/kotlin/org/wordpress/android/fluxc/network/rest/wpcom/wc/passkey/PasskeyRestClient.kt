package org.wordpress.android.fluxc.network.rest.wpcom.wc.passkey

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload

class PasskeyRestClient {
    fun requestWebauthnChallenge(
        userId: Long,
        twoStepNonce: String
    ): WooPayload<WebauthnChallengeInfo> {
        val parameters = mapOf(
            "user_id" to userId.toString(),
            "two_step_nonce" to twoStepNonce,
            "auth_type" to "webauthn"
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

    companion object {
        const val baseURLWithAction = "wp-login.php?action"
        const val challengeEndpoint = "webauthn-challenge-endpoint"
        const val authEndpoint = "webauthn-authentication-endpoint"
    }
}