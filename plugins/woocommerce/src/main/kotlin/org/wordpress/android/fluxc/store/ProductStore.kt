package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.persistence.dao.ProductsDao
import org.wordpress.android.fluxc.persistence.entity.ProductEntity
import org.wordpress.android.fluxc.store.WCProductStore.Companion
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.ProductsDbHelper
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductStore @Inject constructor(
    private val restClient: ProductRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val productsDao: ProductsDao,
    private val productsDbHelper: ProductsDbHelper
) {
    companion object {
        // Just get everything
        const val DEFAULT_PAGE_SIZE = 100
    }

    suspend fun getProducts(siteId: Long) =
        productsDao.getProducts(siteId)

    fun observeProducts(siteId: Long) =
        productsDao.observeProducts(siteId)

    suspend fun getProduct(id: Long) =
        productsDao.getProduct(id)

    fun observeProduct(id: Long) =
        productsDao.observeProduct(id)

    // Returns a boolean indicating whether more coupons can be fetched
    suspend fun fetchProducts(
        site: SiteModel,
        offset: Int = 0,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        sortType: ProductSorting = WCProductStore.DEFAULT_PRODUCT_SORTING,
        includedVariationIds: List<Long> = emptyList(),
        excludedVariationIds: List<Long> = emptyList(),
        filterOptions: Map<ProductFilterOption, String> = emptyMap(),
    ): WooResult<Boolean> {
        return coroutineEngine.withDefaultContext(API, this, "fetchProducts") {
            val response = restClient.fetchProductsWithSyncRequest(
                site = site,
                offset = offset,
                pageSize = pageSize,
                sortType = sortType,
                includedProductIds = includedVariationIds,
                excludedProductIds = excludedVariationIds,
                filterOptions = filterOptions
            )
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    productsDbHelper.insertOrUpdateProducts(site, response.result)
                    val canLoadMore = response.result.size == pageSize
                    WooResult(canLoadMore)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun searchProducts(
        site: SiteModel,
        searchString: String,
        offset: Int = 0,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): WooResult<ProductSearchResult> {
        return coroutineEngine.withDefaultContext(API, this, "searchProducts") {
            val response = restClient.fetchProductsWithSyncRequest(
                site = site,
                offset = offset,
                pageSize = pageSize,
                searchQuery = searchString
            )
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    productsDbHelper.insertOrUpdateProducts(site, response.result)
                    val productIds = response.result.map { it.remoteProductId }
                    val products = productsDao.getProductsByIds(site.siteId, productIds)
                    val canLoadMore = response.result.size == pageSize
                    WooResult(ProductSearchResult(products, canLoadMore))
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    data class ProductSearchResult(
        val products: List<ProductEntity>,
        val canLoadMore: Boolean
    )
}
