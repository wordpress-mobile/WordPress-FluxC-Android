package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.persistence.dao.ProductVariationsDao
import org.wordpress.android.fluxc.persistence.entity.ProductVariationEntity
import org.wordpress.android.fluxc.store.WCProductStore.Companion
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.ProductVariationsDbHelper
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductVariationStore @Inject constructor(
    private val restClient: ProductRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val productVariationsDao: ProductVariationsDao,
    private val productVariationsDbHelper: ProductVariationsDbHelper
) {
    companion object {
        // Just get everything
        const val DEFAULT_PAGE_SIZE = 100
    }

    suspend fun getProductVariations(siteId: Long, productId: Long) =
        productVariationsDao.getProductVariations(siteId, productId)

    fun observeProductVariations(siteId: Long, productId: Long) =
        productVariationsDao.observeProductVariations(siteId, productId)

    suspend fun getProductVariation(id: Long) =
        productVariationsDao.getProductVariation(id)

    fun observeProductVariation(id: Long) =
        productVariationsDao.observeProductVariation(id)

    // Returns a boolean indicating whether more coupons can be fetched
    suspend fun fetchProductsVariations(
        site: SiteModel,
        productId: Long,
        offset: Int = 0,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        includedVariationIds: List<Long> = emptyList(),
        excludedVariationIds: List<Long> = emptyList()
    ): WooResult<Boolean> {
        return coroutineEngine.withDefaultContext(API, this,"fetchProductsVariations") {
            val response = restClient.fetchProductVariationsWithSyncRequest(
                site = site,
                productId = productId,
                offset = offset,
                pageSize = pageSize,
                includedVariationIds = includedVariationIds,
                excludedVariationIds = excludedVariationIds
            )
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    productVariationsDbHelper.insertOrUpdateProductVariations(site, response.result)
                    val canLoadMore = response.result.size == pageSize
                    WooResult(canLoadMore)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun searchProductVariations(
        site: SiteModel,
        productId: Long,
        searchString: String,
        offset: Int = 0,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): WooResult<ProductVariationSearchResult> {
        return coroutineEngine.withDefaultContext(API, this, "searchProductVariations") {
            val response = restClient.fetchProductVariationsWithSyncRequest(
                site = site,
                productId = productId,
                offset = offset,
                pageSize = pageSize,
                searchQuery = searchString
            )
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    productVariationsDbHelper.insertOrUpdateProductVariations(site, response.result)
                    val variationIds = response.result.map { it.remoteVariationId }
                    val variations = productVariationsDao.getProductVariationsByIds(
                        site.siteId,
                        productId,
                        variationIds
                    )
                    val canLoadMore = response.result.size == pageSize
                    WooResult(ProductVariationSearchResult(variations, canLoadMore))
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    data class ProductVariationSearchResult(
        val variations: List<ProductVariationEntity>,
        val canLoadMore: Boolean
    )
}
