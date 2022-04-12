package org.wordpress.android.fluxc.store

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.coupons.CouponReport
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.coupons.CouponDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.coupons.CouponRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.coupons.toDataModel
import org.wordpress.android.fluxc.persistence.TransactionExecutor
import org.wordpress.android.fluxc.persistence.dao.CouponsDao
import org.wordpress.android.fluxc.persistence.dao.ProductCategoriesDao
import org.wordpress.android.fluxc.persistence.dao.ProductsDao
import org.wordpress.android.fluxc.persistence.entity.CouponAndProductCategoryEntity
import org.wordpress.android.fluxc.persistence.entity.CouponAndProductEntity
import org.wordpress.android.fluxc.persistence.entity.CouponDataModel
import org.wordpress.android.fluxc.persistence.entity.CouponEmailEntity
import org.wordpress.android.fluxc.persistence.entity.CouponWithEmails
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.AppLog.T.API
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CouponStore @Inject constructor(
    private val restClient: CouponRestClient,
    private val couponsDao: CouponsDao,
    private val productsDao: ProductsDao,
    private val productCategoriesDao: ProductCategoriesDao,
    private val coroutineEngine: CoroutineEngine,
    private val productStore: WCProductStore,
    private val database: TransactionExecutor
) {
    companion object {
        // Just get everything
        const val DEFAULT_PAGE_SIZE = 100
        const val DEFAULT_PAGE = 1
    }

    // Returns a boolean indicating whether more coupons can be fetched
    suspend fun fetchCoupons(
        site: SiteModel,
        page: Int = DEFAULT_PAGE,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): WooResult<Boolean> {
        return coroutineEngine.withDefaultContext(API, this, "fetchCoupons") {
            val response = restClient.fetchCoupons(site, page, pageSize)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    database.executeInTransaction {
                        // clear the table if the 1st page is requested
                        if (page == 1) {
                            couponsDao.deleteAllCoupons(site.siteId)
                        }

                        response.result.forEach { addCouponToDatabase(it, site) }
                    }
                    val canLoadMore = response.result.size == pageSize
                    WooResult(canLoadMore)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun fetchCoupon(
        site: SiteModel,
        couponId: Long
    ): WooResult<Unit> {
        return coroutineEngine.withDefaultContext(API, this, "fetchCoupon") {
            val response = restClient.fetchCoupon(site, couponId)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    addCouponToDatabase(response.result, site)
                    WooResult(Unit)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    private suspend fun addCouponToDatabase(dto: CouponDto, site: SiteModel) {
        database.executeInTransaction {
            couponsDao.insertOrUpdateCoupon(dto.toDataModel(site.siteId))
            insertRelatedProducts(dto, site)
            insertRelatedProductCategories(dto, site)
            insertRestrictedEmailAddresses(dto, site)
        }
    }

    suspend fun deleteCoupon(
        site: SiteModel,
        couponId: Long,
        trash: Boolean = true
    ): WooResult<Unit> {
        return coroutineEngine.withDefaultContext(T.API, this, "deleteCoupon") {
            val result = restClient.deleteCoupon(site, couponId, trash)

            return@withDefaultContext if (result.isError) {
                WooResult(result.error)
            } else {
                couponsDao.deleteCoupon(site.siteId, couponId)
                WooResult(Unit)
            }
        }
    }

    private suspend fun insertRestrictedEmailAddresses(dto: CouponDto, site: SiteModel) {
        dto.restrictedEmails?.forEach { email ->
            couponsDao.insertOrUpdateCouponEmail(
                CouponEmailEntity(
                    couponId = dto.id,
                    siteId = site.siteId,
                    email = email
                )
            )
        }
    }

    private suspend fun insertRelatedProductCategories(dto: CouponDto, site: SiteModel) {
        fetchMissingProductCategories(dto.productCategoryIds, site)
        fetchMissingProductCategories(dto.excludedProductCategoryIds, site)

        dto.productCategoryIds?.forEach { categoryId ->
            couponsDao.insertOrUpdateCouponAndProductCategory(
                CouponAndProductCategoryEntity(
                    couponId = dto.id,
                    siteId = site.siteId,
                    productCategoryId = categoryId,
                    isExcluded = false
                )
            )
        }

        dto.excludedProductCategoryIds?.forEach { categoryId ->
            couponsDao.insertOrUpdateCouponAndProductCategory(
                CouponAndProductCategoryEntity(
                    couponId = dto.id,
                    siteId = site.siteId,
                    productCategoryId = categoryId,
                    isExcluded = true
                )
            )
        }
    }

    private suspend fun insertRelatedProducts(dto: CouponDto, site: SiteModel) {
        fetchMissingProducts(dto.productIds, site)
        fetchMissingProducts(dto.excludedProductIds, site)

        dto.productIds?.forEach { productId ->
            couponsDao.insertOrUpdateCouponAndProduct(
                CouponAndProductEntity(
                    couponId = dto.id,
                    siteId = site.siteId,
                    productId = productId,
                    isExcluded = false
                )
            )
        }

        dto.excludedProductIds?.forEach { productId ->
            couponsDao.insertOrUpdateCouponAndProduct(
                CouponAndProductEntity(
                    couponId = dto.id,
                    siteId = site.siteId,
                    productId = productId,
                    isExcluded = true
                )
            )
        }
    }

    fun observeCoupon(site: SiteModel, couponId: Long): Flow<CouponDataModel?> =
        couponsDao.observeCoupon(site.siteId, couponId)
            .map { coupon ->
                coupon?.let {
                    assembleCouponDataModel(site, it)
                }
            }
            .distinctUntilChanged()

    @ExperimentalCoroutinesApi
    fun observeCoupons(site: SiteModel): Flow<List<CouponDataModel>> =
        couponsDao.observeCoupons(site.siteId)
            .mapLatest { list ->
                list.map { assembleCouponDataModel(site, it) }
            }
            .distinctUntilChanged()

    suspend fun fetchCouponReport(site: SiteModel, couponId: Long): WooResult<CouponReport> =
        coroutineEngine.withDefaultContext(T.API, this, "fetchCouponReport") {
            // Old date, 1 second since epoch
            val date = Date(TimeUnit.SECONDS.toMillis(1))

            return@withDefaultContext restClient.fetchCouponReport(site, couponId, date)
                .let { result ->
                    if (result.isError) {
                        WooResult(result.error)
                    } else {
                        WooResult(result.result!!.toDataModel())
                    }
                }
        }

    private fun assembleCouponDataModel(site: SiteModel, it: CouponWithEmails): CouponDataModel {
        val includedProducts = productsDao.getCouponProducts(
            siteId = site.siteId,
            couponId = it.couponEntity.id,
            areExcluded = false
        )
        val excludedProducts = productsDao.getCouponProducts(
            siteId = site.siteId,
            couponId = it.couponEntity.id,
            areExcluded = true
        )
        val includedCategories = productCategoriesDao.getCouponProductCategories(
            siteId = site.siteId,
            couponId = it.couponEntity.id,
            areExcluded = false
        )
        val excludedCategories = productCategoriesDao.getCouponProductCategories(
            siteId = site.siteId,
            couponId = it.couponEntity.id,
            areExcluded = true
        )
        return CouponDataModel(
            it.couponEntity,
            includedProducts,
            excludedProducts,
            includedCategories,
            excludedCategories,
            it.restrictedEmails
        )
    }

    private suspend fun fetchMissingProducts(productIds: List<Long>?, site: SiteModel) {
        if (!productIds.isNullOrEmpty()) {
            val products = productsDao.getProductsByIds(site.siteId, productIds).map { it.id }

            // find missing products
            val missingIds = productIds - products
            if (missingIds.isNotEmpty()) {
                productStore.fetchProductListSynced(site, missingIds)
            }
        }
    }

    private suspend fun fetchMissingProductCategories(categoryIds: List<Long>?, site: SiteModel) {
        if (!categoryIds.isNullOrEmpty()) {
            val categories = productCategoriesDao.getProductCategoriesByIds(
                site.siteId,
                categoryIds
            ).map { it.id }

            // find missing product categories
            val missingIds = categoryIds - categories
            if (missingIds.isNotEmpty()) {
                productStore.fetchProductCategoryListSynced(site, missingIds)
            }
        }
    }
}
