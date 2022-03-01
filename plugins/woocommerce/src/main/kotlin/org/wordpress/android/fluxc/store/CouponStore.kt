package org.wordpress.android.fluxc.store

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.coupons.CouponDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.coupons.CouponRestClient
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.dao.CouponsDao
import org.wordpress.android.fluxc.persistence.dao.ProductCategoriesDao
import org.wordpress.android.fluxc.persistence.dao.ProductsDao
import org.wordpress.android.fluxc.persistence.entity.CouponAndProductCategoryEntity
import org.wordpress.android.fluxc.persistence.entity.CouponAndProductEntity
import org.wordpress.android.fluxc.persistence.entity.CouponDataModel
import org.wordpress.android.fluxc.persistence.entity.CouponEmailEntity
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.AppLog.T.API
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
    private val database: WCAndroidDatabase
) {
    companion object {
        // Just get everything
        const val DEFAULT_PAGE_SIZE = 100
        const val DEFAULT_PAGE = 1
    }

    suspend fun fetchCoupons(
        site: SiteModel,
        page: Int = DEFAULT_PAGE,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): WooResult<Unit> {
        return coroutineEngine.withDefaultContext(API, this, "fetchCoupons") {
            val response = restClient.fetchCoupons(site, page, pageSize)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    response.result.forEach { dto ->
                        database.runInTransaction {
                            coroutineEngine.launch(
                                T.DB,
                                this,
                                "fetchCoupons DB transaction"
                            ) {
                                couponsDao.insertOrUpdateCoupon(dto.toDataModel(site.siteId))
                                insertRelatedProducts(dto, site)
                                insertRelatedProductCategories(dto, site)
                                insertRestrictedEmailAddresses(dto, site)
                            }
                        }
                    }

                    WooResult(Unit)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    private suspend fun insertRestrictedEmailAddresses(
        dto: CouponDto,
        site: SiteModel
    ) {
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

    fun observeCoupons(site: SiteModel): Flow<List<CouponDataModel>> =
        couponsDao.observeCoupons(site.siteId)
            .mapLatest { list ->
                list.map {
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
                        areExcluded = true
                    )
                    val excludedCategories = productCategoriesDao.getCouponProductCategories(
                        siteId = site.siteId,
                        couponId = it.couponEntity.id,
                        areExcluded = false
                    )
                    CouponDataModel(
                        it.couponEntity,
                        includedProducts,
                        excludedProducts,
                        includedCategories,
                        excludedCategories,
                        it.restrictedEmails
                    )
                }
            }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()

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
