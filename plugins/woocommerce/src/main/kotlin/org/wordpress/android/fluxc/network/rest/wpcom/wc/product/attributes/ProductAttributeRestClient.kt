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
import org.wordpress.android.fluxc.utils.syncDeleteRequest
import org.wordpress.android.fluxc.utils.syncPutRequest
import javax.inject.Singleton

@Singleton
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

    suspend fun postNewAttribute(
        site: SiteModel,
        args: Map<String, String>
    ) = WOOCOMMERCE.products.attributes.pathV3
            .postTo(site, args)
            .handleResult()

    suspend fun updateExistingAttribute(
        site: SiteModel,
        attributeID: Long,
        args: Map<String, String>
    ) = WOOCOMMERCE.products.attributes.attribute(attributeID).pathV3
            .putTo(site, args)
            .handleResult()

    suspend fun deleteExistingAttribute(
        site: SiteModel,
        attributeID: Long
    ) = WOOCOMMERCE.products.attributes.attribute(attributeID).pathV3
            .deleteFrom(site)
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

    private suspend fun String.putTo(
        site: SiteModel,
        args: Map<String, String>
    ) = jetpackTunnelGsonRequestBuilder.syncPutRequest(
            this@ProductAttributeRestClient,
            site,
            this,
            args,
            AttributeApiResponse::class.java
    )

    private suspend fun String.deleteFrom(
        site: SiteModel
    ) = jetpackTunnelGsonRequestBuilder.syncDeleteRequest(
            this@ProductAttributeRestClient,
            site,
            this,
            AttributeApiResponse::class.java
    )
}
