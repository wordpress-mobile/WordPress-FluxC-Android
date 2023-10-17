package org.wordpress.android.fluxc.network.rest.wpcom.wc.passkey

data class WebauthnChallengeInfo(
    val challenge: String,
    val rpId: String,
    val twoStepNonce: String,
    val allowedCredentials: List<String>
)
