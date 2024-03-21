package org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn

import com.google.gson.annotations.SerializedName
import org.json.JSONObject

data class WebauthnData(
    val passKeyData: JSONObject,
    val securityKeyChallengeInfo: WebauthnChallengeInfo? = null
)
class WebauthnChallengeInfo(
    val challenge: String,
    val rpId: String,
    val allowCredentials: List<WebauthnCredentialResponse>,
    val timeout: Int,
    @SerializedName("two_step_nonce")
    val twoStepNonce: String
)

class WebauthnCredentialResponse(
    val type: String,
    val id: String,
    val transports: List<String>
)