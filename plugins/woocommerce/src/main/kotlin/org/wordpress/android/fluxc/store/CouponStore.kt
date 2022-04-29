package org.wordpress.android.fluxc.store

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.coupon.UpdateCouponRequest
import org.wordpress.android.fluxc.model.coupons.CouponReport
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_PARAM
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

    suspend fun searchCoupons(
        site: SiteModel,
        searchString: String,
        page: Int = DEFAULT_PAGE,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): WooResult<CouponSearchResult> {
        return coroutineEngine.withDefaultContext(API, this, "searchCoupons") {
            val response = restClient.fetchCoupons(site, page, pageSize, searchString)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    database.executeInTransaction {
                        response.result.forEach { addCouponToDatabase(it, site) }
                    }

                    val couponIds = response.result.map { it.id }
                    val coupons = couponsDao.getCoupons(site.siteId, couponIds)
                    val canLoadMore = response.result.size == pageSize
                    WooResult(
                        CouponSearchResult(
                            coupons.map { assembleCouponDataModel(site, it) },
                            canLoadMore
                        )
                    )
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun fetchCoupon(site: SiteModel, couponId: Long): WooResult<Unit> {
        return coroutineEngine.withDefaultContext(API, this, "fetchCoupon") {
            val response = restClient.fetchCoupon(site, couponId)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    addCouponToDatabase(response.result, site)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    private suspend fun addCouponToDatabase(dto: CouponDto, site: SiteModel) =
        try {
            database.executeInTransaction {
                couponsDao.insertOrUpdateCoupon(dto.toDataModel(site.siteId))
                insertRelatedProducts(dto, site)
                insertRelatedCategories(dto, site)
                insertRestrictedEmailAddresses(dto, site)
            }
            WooResult()
        } catch (e: MissingRelatedEntityException) {
            WooResult<Unit>(WooError(GENERIC_ERROR, UNKNOWN, e.toString()))
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

    private suspend fun insertRelatedCategories(dto: CouponDto, site: SiteModel) {
        dto.productCategoryIds?.let {
            fetchAndInsertCategories(
                site = site,
                couponId = dto.id,
                categoryIds = dto.productCategoryIds,
                areExcluded = false
            )
        }

        dto.excludedProductCategoryIds?.let {
            fetchAndInsertCategories(
                site = site,
                couponId = dto.id,
                categoryIds = dto.excludedProductCategoryIds,
                areExcluded = true
            )
        }
    }

    private suspend fun fetchAndInsertCategories(
        site: SiteModel,
        couponId: Long,
        categoryIds: List<Long>,
        areExcluded: Boolean
    ) {
        fetchMissingProductCategories(categoryIds, site)

        val dbCategoryIds = productCategoriesDao
            .getProductCategoriesByIds(site.siteId, categoryIds)
            .map { it.id }

        if (dbCategoryIds.sorted() != categoryIds.sorted()) {
            throw MissingRelatedEntityException(
                "ProductCategoryEntity",
                couponId,
                categoryIds - dbCategoryIds,
                areExcluded
            )
        }

        categoryIds.forEach { categoryId ->
            couponsDao.insertOrUpdateCouponAndProductCategory(
                CouponAndProductCategoryEntity(
                    couponId = couponId,
                    siteId = site.siteId,
                    productCategoryId = categoryId,
                    isExcluded = areExcluded
                )
            )
        }
    }

    private suspend fun insertRelatedProducts(dto: CouponDto, site: SiteModel) {
        dto.productIds?.let {
            fetchAndInsertProducts(
                site = site,
                couponId = dto.id,
                productIds = dto.productIds,
                areExcluded = false
            )
        }

        dto.excludedProductIds?.let {
            fetchAndInsertProducts(
                site = site,
                couponId = dto.id,
                productIds = dto.excludedProductIds,
                areExcluded = true
            )
        }
    }

    private suspend fun fetchAndInsertProducts(
        site: SiteModel,
        couponId: Long,
        productIds: List<Long>,
        areExcluded: Boolean
    ) {
        fetchMissingProducts(productIds, site)

        val dbProductIds = productsDao
            .getProductsByIds(site.siteId, productIds)
            .map { it.id }

        if (dbProductIds.sorted() != productIds.sorted()) {
            throw MissingRelatedEntityException(
                "ProductEntity",
                couponId,
                productIds - dbProductIds,
                areExcluded
            )
        }

        productIds.forEach { productId ->
            couponsDao.insertOrUpdateCouponAndProduct(
                CouponAndProductEntity(
                    couponId = couponId,
                    siteId = site.siteId,
                    productId = productId,
                    isExcluded = areExcluded
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

    suspend fun getCoupon(site: SiteModel, couponId: Long): CouponDataModel? =
        couponsDao.getCoupon(site.siteId, couponId)?.let {
            assembleCouponDataModel(site, it)
        }

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

    suspend fun updateCoupon(
        couponId: Long,
        site: SiteModel,
        updateCouponRequest: UpdateCouponRequest
    ): WooResult<Unit> {
        return if (updateCouponRequest.code.isNullOrEmpty()) {
            WooResult(
                WooError(
                    type = INVALID_PARAM,
                    original = UNKNOWN,
                    message = "Coupon code cannot be empty"
                )
            )
        } else {
            coroutineEngine.withDefaultContext(T.API, this, "createCoupon") {
                return@withDefaultContext restClient.updateCoupon(site, couponId, updateCouponRequest)
                    .let { response ->
                        if (response.isError || response.result == null) {
                            WooResult(response.error)
                        } else {
                            addCouponToDatabase(response.result, site)
                            WooResult(Unit)
                        }
                    }
            }
        }
    }

    private fun assembleCouponDataModel(site: SiteModel, it: CouponWithEmails): CouponDataModel {
        val includedProducts = productsDao.getCouponProducts(
            siteId = site.siteId,
            couponId = it.coupon.id,
            areExcluded = false
        )
        val excludedProducts = productsDao.getCouponProducts(
            siteId = site.siteId,
            couponId = it.coupon.id,
            areExcluded = true
        )
        val includedCategories = productCategoriesDao.getCouponProductCategories(
            siteId = site.siteId,
            couponId = it.coupon.id,
            areExcluded = false
        )
        val excludedCategories = productCategoriesDao.getCouponProductCategories(
            siteId = site.siteId,
            couponId = it.coupon.id,
            areExcluded = true
        )
        return CouponDataModel(
            it.coupon,
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

    data class CouponSearchResult(
        val coupons: List<CouponDataModel>,
        val canLoadMore: Boolean
    )

    class MissingRelatedEntityException(
        entityName: String,
        couponId: Long,
        missingIds: List<Long>,
        areExcluded: Boolean
    ): Exception(
        "Some${if (areExcluded) " excluded " else " "}entities ($entityName) " +
            "referenced by a coupon ($couponId) are missing: [${missingIds.joinToString()}]"
    )
}
