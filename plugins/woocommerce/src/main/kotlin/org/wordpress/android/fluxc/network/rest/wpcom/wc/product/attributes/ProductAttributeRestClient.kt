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
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.terms.AttributeTermApiResponse
import org.wordpress.android.fluxc.utils.handleResult
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
            .request<Array<AttributeApiResponse>>(site)

    suspend fun postNewAttribute(
        site: SiteModel,
        args: Map<String, String>
    ) = WOOCOMMERCE.products.attributes.pathV3
            .post<AttributeApiResponse>(site, args)

    suspend fun updateExistingAttribute(
        site: SiteModel,
        attributeID: Long,
        args: Map<String, String>
    ) = WOOCOMMERCE.products.attributes.attribute(attributeID).pathV3
            .put<AttributeApiResponse>(site, args)

    suspend fun deleteExistingAttribute(
        site: SiteModel,
        attributeID: Long
    ) = WOOCOMMERCE.products.attributes.attribute(attributeID).pathV3
            .delete<AttributeApiResponse>(site)

    suspend fun fetchAllAttributeTerms(
        site: SiteModel,
        attributeID: Long
    ) = WOOCOMMERCE.products.attributes.attribute(attributeID).terms.pathV3
            .request<Array<AttributeTermApiResponse>>(site)

    suspend fun postNewTerm(
        site: SiteModel,
        args: Map<String, String>,
        attributeID: Long
    ) = WOOCOMMERCE.products.attributes.attribute(attributeID).terms.pathV3
            .post<AttributeTermApiResponse>(site, args)

    suspend fun updateExistingTerm(
        site: SiteModel,
        args: Map<String, String>,
        attributeID: Long,
        termID: Long
    ) = WOOCOMMERCE.products.attributes.attribute(attributeID).terms.term(termID).pathV3
            .put<AttributeTermApiResponse>(site, args)

    suspend fun deleteExistingTerm(
        site: SiteModel,
        args: Map<String, String>,
        attributeID: Long,
        termID: Long
    ) = WOOCOMMERCE.products.attributes.attribute(attributeID).terms.term(termID).pathV3
            .delete<AttributeTermApiResponse>(site)

    private suspend inline fun <reified T : Any> String.request(
        site: SiteModel
    ) = jetpackTunnelGsonRequestBuilder.syncGetRequest(
            this@ProductAttributeRestClient,
            site,
            this,
            emptyMap(),
            T::class.java
    ).handleResult()

    private suspend inline fun <reified T : Any> String.post(
        site: SiteModel,
        args: Map<String, String>
    ) = jetpackTunnelGsonRequestBuilder.syncPostRequest(
            this@ProductAttributeRestClient,
            site,
            this,
            args,
            T::class.java
    ).handleResult()

    private suspend inline fun <reified T : Any> String.put(
        site: SiteModel,
        args: Map<String, String>
    ) = jetpackTunnelGsonRequestBuilder.syncPutRequest(
            this@ProductAttributeRestClient,
            site,
            this,
            args,
            T::class.java
    ).handleResult()

    private suspend inline fun <reified T : Any> String.delete(
        site: SiteModel
    ) = jetpackTunnelGsonRequestBuilder.syncDeleteRequest(
            this@ProductAttributeRestClient,
            site,
            this,
            T::class.java
    ).handleResult()
}
