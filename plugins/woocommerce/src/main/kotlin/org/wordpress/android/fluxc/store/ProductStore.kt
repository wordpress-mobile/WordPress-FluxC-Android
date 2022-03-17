package org.wordpress.android.fluxc.store

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.coupons.CouponDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.WCProductRestClient
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.dao.CouponsDao
import org.wordpress.android.fluxc.persistence.dao.ProductCategoriesDao
import org.wordpress.android.fluxc.persistence.dao.ProductsDao
import org.wordpress.android.fluxc.persistence.entity.CouponAndProductCategoryEntity
import org.wordpress.android.fluxc.persistence.entity.CouponAndProductEntity
import org.wordpress.android.fluxc.persistence.entity.CouponDataModel
import org.wordpress.android.fluxc.persistence.entity.CouponEmailEntity
import org.wordpress.android.fluxc.persistence.entity.ProductEntity
import org.wordpress.android.fluxc.store.ProductsStore.FetchProductsError
import org.wordpress.android.fluxc.store.ProductsStore.FetchProductsErrorType
import org.wordpress.android.fluxc.store.ProductsStore.OnProductsFetched
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductStore @Inject constructor(
    private val restClient: ProductRestClient,
    private val couponsDao: CouponsDao,
    private val productsDao: ProductsDao,
    private val productCategoriesDao: ProductCategoriesDao,
    private val coroutineEngine: CoroutineEngine,
    private val productStore: WCProductStore,
    private val database: WCAndroidDatabase
) {
    companion object {
        // Just get everything
        const val DEFAULT_PAGE_SIZE = 100
        const val DEFAULT_PAGE = 1
    }

//    suspend fun fetchProducts(site: SiteModel, productIds: List<Long>): List<ProductEntity>? {
//        return coroutineEngine.withDefaultContext(API, this, "fetchProducts") {
//            restClient.fetchProductsWithSyncRequest(site = site, remoteProductIds = productIds).result
//        }?.also {
//            productsDao.insertOrUpdateProducts(it)
//        }
//    }

//    suspend fun fetchProducts(
//        site: SiteModel,
//        page: Int = DEFAULT_PAGE,
//        pageSize: Int = DEFAULT_PAGE_SIZE
//    ): WooResult<Unit> {
//        coroutineEngine.withDefaultContext(T.API, this, "Fetch products") {
//            return@withDefaultContext when (val response = restClient.fetchProducts(site, pageSize, page)) {
//                is Success -> {
//                    OnProductsFetched(response.data.products)
//                }
//                is Error -> {
//                    OnProductsFetched(FetchProductsError(FetchProductsErrorType.GENERIC_ERROR, response.error.message))
//                }
//            }
//        }
//    }
}
