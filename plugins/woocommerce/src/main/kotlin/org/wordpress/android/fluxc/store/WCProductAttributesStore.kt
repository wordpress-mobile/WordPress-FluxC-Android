package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.product.attributes.WCProductAttributeMapper
import org.wordpress.android.fluxc.model.product.attributes.WCProductAttributeModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.ProductAttributeRestClient
import org.wordpress.android.fluxc.persistence.WCProductAttributeSqlUtils.deleteSingleStoredAttribute
import org.wordpress.android.fluxc.persistence.WCProductAttributeSqlUtils.getCurrentAttributes
import org.wordpress.android.fluxc.persistence.WCProductAttributeSqlUtils.insertFromScratchCompleteAttributesList
import org.wordpress.android.fluxc.persistence.WCProductAttributeSqlUtils.insertSingleAttribute
import org.wordpress.android.fluxc.persistence.WCProductAttributeSqlUtils.updateSingleStoredAttribute
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCProductAttributesStore @Inject constructor(
    private val restClient: ProductAttributeRestClient,
    private val mapper: WCProductAttributeMapper,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchStoreAttributes(
        site: SiteModel
    ): WooResult<List<WCProductAttributeModel>> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchStoreAttributes") {
                restClient.fetchProductFullAttributesList(site)
                        .asWooResult()
                        .model
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { mapper.responseToAttributeModelList(it, site) }
                        ?.let {
                            insertFromScratchCompleteAttributesList(it, site.id)
                            getCurrentAttributes(site.id)
                        }
                        ?.let { WooResult(it) }
                        ?: getCurrentAttributes(site.id)
                                .takeIf { it.isNotEmpty() }
                                ?.let { WooResult(it) }
                        ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }

    suspend fun createAttribute(
        site: SiteModel,
        name: String,
        slug: String = name,
        type: String = "select",
        orderBy: String = "menu_order",
        hasArchives: Boolean = false
    ): WooResult<WCProductAttributeModel> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "createStoreAttributes") {
                restClient.postNewAttribute(
                        site, mapOf(
                        "name" to name,
                        "slug" to slug,
                        "type" to type,
                        "order_by" to orderBy,
                        "has_archives" to hasArchives.toString())
                )
                        .asWooResult()
                        .model
                        ?.let { mapper.responseToAttributeModel(it, site) }
                        ?.let { insertSingleAttribute(it) }
                        ?.let { WooResult(it) }
                        ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }

    suspend fun updateAttribute(
        site: SiteModel,
        attributeID: Long,
        name: String,
        slug: String = name,
        type: String = "select",
        orderBy: String = "menu_order",
        hasArchives: Boolean = false
    ): WooResult<WCProductAttributeModel> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "updateStoreAttributes") {
                restClient.updateExistingAttribute(
                        site, attributeID, mapOf(
                        "id" to attributeID.toString(),
                        "name" to name,
                        "slug" to slug,
                        "type" to type,
                        "order_by" to orderBy,
                        "has_archives" to hasArchives.toString())
                )
                        .asWooResult()
                        .model
                        ?.let { mapper.responseToAttributeModel(it, site) }
                        ?.let { updateSingleStoredAttribute(it, site.id) }
                        ?.let { WooResult(it) }
                        ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }

    suspend fun deleteAttribute(
        site: SiteModel,
        attributeID: Long
    ): WooResult<WCProductAttributeModel> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "deleteStoreAttributes") {
                restClient.deleteExistingAttribute(site, attributeID)
                        .asWooResult()
                        .model
                        ?.let { mapper.responseToAttributeModel(it, site) }
                        ?.let { deleteSingleStoredAttribute(it, site.id) }
                        ?.let { WooResult(it) }
                        ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }

    private fun <T> WooPayload<T>.asWooResult() = when {
        isError -> WooResult(error)
        result != null -> WooResult<T>(result)
        else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
    }
}
