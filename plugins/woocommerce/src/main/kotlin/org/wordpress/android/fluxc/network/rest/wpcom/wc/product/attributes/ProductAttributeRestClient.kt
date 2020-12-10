package org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.utils.handleResult

class ProductAttributeRestClient
constructor(
    appContext: Context?,
    dispatcher: Dispatcher,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchProductFullAttributesList(
        site: SiteModel
    ) = WOOCOMMERCE.products.attributes.pathV3
            .requestTo(site)
            .handleResult()

    suspend fun createProductSingleAttribute(
        site: SiteModel,
        args: Map<String, String>
    ) = WOOCOMMERCE.products.attributes.pathV3
            .postTo(site, args)
            .handleResult()

    private suspend fun String.requestTo(
        site: SiteModel
    ) = jetpackTunnelGsonRequestBuilder.syncGetRequest(
            this@ProductAttributeRestClient,
            site,
            this,
            emptyMap(),
            Array<AttributeApiResponse>::class.java
    )

    private suspend fun String.postTo(
        site: SiteModel,
        args: Map<String, String>
    ) = jetpackTunnelGsonRequestBuilder.syncPostRequest(
            this@ProductAttributeRestClient,
            site,
            this,
            args,
            AttributeApiResponse::class.java
    )
}
