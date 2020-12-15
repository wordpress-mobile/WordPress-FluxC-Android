package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
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
    ): WooResult<List<AttributeApiResponse>> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchStoreAttributes") {
                acquireCurrentData(site)
            }

    suspend fun acquireCurrentData(
        site: SiteModel
    ) = with(restClient.fetchProductFullAttributesList(site)) {
        when {
            isError -> WooResult(error)
            result != null -> WooResult(result.toList())
            else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
        }
    }
}
