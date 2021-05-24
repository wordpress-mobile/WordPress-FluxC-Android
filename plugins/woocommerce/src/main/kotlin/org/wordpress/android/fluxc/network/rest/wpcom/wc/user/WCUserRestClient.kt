package org.wordpress.android.fluxc.network.rest.wpcom.wc.user

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPAPI
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.handleResult
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class WCUserRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchUserInfo(
        site: SiteModel
    ): WooPayload<UserApiResponse> {
        val url = WPAPI.users.me.urlV2
        val params = mapOf(
                "context" to "edit"
        )

        return jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                params,
                UserApiResponse::class.java
        ).handleResult()
    }

    data class UserApiResponse(
        val id: Long,
        val username: String?,
        val name: String?,
        @SerializedName("first_name") val firstName: String?,
        @SerializedName("last_name") val lastName: String?,
        val email: String?,
        val roles: JsonElement
    )
}
