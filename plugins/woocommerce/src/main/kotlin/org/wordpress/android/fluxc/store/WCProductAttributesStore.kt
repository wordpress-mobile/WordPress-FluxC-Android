package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.AttributeApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.ProductAttributeRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCProductAttributesStore @Inject constructor(
    private val restClient: ProductAttributeRestClient,
    private val productStore: WCProductStore,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchStoreAttributes(
        site: SiteModel
    ): WooResult<Array<AttributeApiResponse>> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchStoreAttributes") {
                restClient.fetchProductFullAttributesList(site)
                        .toWooResult()
            }

    suspend fun createAttribute(
        site: SiteModel,
        name: String,
        type: String = "select",
        orderBy: String = "menu_order",
        hasArchives: Boolean = false
    ): WooResult<AttributeApiResponse> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "createStoreAttributes") {
                restClient.postNewAttribute(
                        site, mapOf(
                        "name" to name,
                        "slug" to name,
                        "type" to type,
                        "order_by" to orderBy,
                        "has_archives" to hasArchives.toString()
                )).toWooResult()
            }

    suspend fun updateAttribute(
        site: SiteModel,
        attributeID: Long,
        name: String,
        type: String = "select",
        orderBy: String = "menu_order",
        hasArchives: Boolean = false
    ): WooResult<AttributeApiResponse> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "updateStoreAttributes") {
                restClient.updateExistingAttribute(
                        site, attributeID, mapOf(
                        "id" to attributeID.toString(),
                        "name" to name,
                        "slug" to name,
                        "type" to type,
                        "order_by" to orderBy,
                        "has_archives" to hasArchives.toString()
                )
                ).toWooResult()
            }

    suspend fun deleteAttribute(
        site: SiteModel,
        attributeID: Long
    ): WooResult<AttributeApiResponse> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "deleteStoreAttributes") {
                restClient.deleteExistingAttribute(site, attributeID)
                        .toWooResult()
            }

    private fun <T> WooPayload<T>.toWooResult(
    ) = when {
        isError -> WooResult(error)
        result != null -> WooResult<T>(result)
        else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
    }
}
