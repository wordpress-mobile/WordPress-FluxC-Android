package org.wordpress.android.fluxc.network.rest.wpcom.wc.passkey

class PasskeyRestClient {
    companion object {
        const val baseURLWithAction = "wp-login.php?action"
        const val challengeEndpoint = "webauthn-challenge-endpoint"
        const val authEndpoint = "webauthn-authentication-endpoint"
    }
}