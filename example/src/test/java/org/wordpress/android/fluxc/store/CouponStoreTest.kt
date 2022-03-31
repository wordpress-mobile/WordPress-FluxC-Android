package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
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
import org.wordpress.android.fluxc.persistence.entity.CouponEntity
import org.wordpress.android.fluxc.persistence.entity.CouponWithEmails
import org.wordpress.android.fluxc.persistence.entity.ProductCategoryEntity
import org.wordpress.android.fluxc.persistence.entity.ProductEntity
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class CouponStoreTest {
    @Mock private lateinit var restClient: CouponRestClient
    @Mock private lateinit var couponsDao: CouponsDao
    @Mock private lateinit var productsDao: ProductsDao
    @Mock private lateinit var productCategoriesDao: ProductCategoriesDao
    @Mock private lateinit var productStore: WCProductStore
    @Mock private lateinit var database: WCAndroidDatabase

    private lateinit var couponStore: CouponStore

    private val site = SiteModel().apply { siteId = 123 }

    private val couponDto = CouponDto(
        id = 1L,
        code = "CODE",
        amount = "10",
        dateCreated = "2021-12-27 11:33:55",
        dateCreatedGmt = "2021-12-27 11:33:55Z",
        dateModified = "2021-12-27 11:33:55",
        dateModifiedGmt = "2021-12-27 11:33:55Z",
        discountType = "percent",
        description = "Description",
        dateExpires = "2023-12-27 11:33:55",
        dateExpiresGmt = "2023-12-27 11:33:55Z",
        usageCount = 1,
        isForIndividualUse = false,
        productIds = listOf(2L, 3L),
        excludedProductIds = listOf(4L, 5L),
        usageLimit = 3,
        usageLimitPerUser = 1,
        limitUsageToXItems = 2,
        isShippingFree = false,
        productCategoryIds = listOf(2L, 3L),
        excludedProductCategoryIds = listOf(2L, 3L),
        areSaleItemsExcluded = true,
        minimumAmount = "5",
        maximumAmount = "50",
        restrictedEmails = listOf("email@email.com"),
        usedBy = null
    )

    private val expectedCoupon = CouponEntity(
        id = couponDto.id,
        siteId = site.siteId,
        code = couponDto.code,
        amount = couponDto.amount,
        dateCreated = couponDto.dateCreated,
        dateCreatedGmt = couponDto.dateCreatedGmt,
        dateModified = couponDto.dateModified,
        dateModifiedGmt = couponDto.dateModifiedGmt,
        discountType = couponDto.discountType,
        description = couponDto.description,
        dateExpires = couponDto.dateExpires,
        dateExpiresGmt = couponDto.dateExpiresGmt,
        usageCount = couponDto.usageCount,
        isForIndividualUse = couponDto.isForIndividualUse,
        usageLimit = couponDto.usageLimit,
        usageLimitPerUser = couponDto.usageLimitPerUser,
        limitUsageToXItems = couponDto.limitUsageToXItems,
        isShippingFree = couponDto.isShippingFree,
        areSaleItemsExcluded = couponDto.areSaleItemsExcluded,
        minimumAmount = couponDto.minimumAmount,
        maximumAmount = couponDto.maximumAmount
    )

    private val expectedEmail = CouponEmailEntity(
        couponId = couponDto.id,
        siteId = site.siteId,
        email = couponDto.restrictedEmails!!.first()
    )

    private val includedProduct1 = ProductEntity(
        id = couponDto.productIds!![0],
        siteId = site.siteId,
        name = "Included Prod 1"
    )

    private val includedProduct2 = ProductEntity(
        id = couponDto.productIds!![1],
        siteId = site.siteId,
        name = "Included Prod 2"
    )

    private val excludedProduct1 = ProductEntity(
        id = couponDto.excludedProductIds!![0],
        siteId = site.siteId,
        name = "Excluded Prod 3"
    )

    private val excludedProduct2 = ProductEntity(
        id = couponDto.excludedProductIds!![1],
        siteId = site.siteId,
        name = "Excluded Prod 4"
    )

    private val includedCategory1 = ProductCategoryEntity(
        id = couponDto.productCategoryIds!![0],
        siteId = site.siteId,
        name = "Included Cat 1"
    )

    private val includedCategory2 = ProductCategoryEntity(
        id = couponDto.productCategoryIds!![1],
        siteId = site.siteId,
        name = "Included Cat 2"
    )

    private val excludedCategory1 = ProductCategoryEntity(
        id = couponDto.excludedProductCategoryIds!![0],
        siteId = site.siteId,
        name = "Excluded Cat 3"
    )

    private val excludedCategory2 = ProductCategoryEntity(
        id = couponDto.excludedProductCategoryIds!![1],
        siteId = site.siteId,
        name = "Excluded Cat 4"
    )

    private val expectedIncludedCouponAndProduct1 = CouponAndProductEntity(
        couponId = couponDto.id,
        siteId = site.siteId,
        productId = couponDto.productIds!![0],
        isExcluded = false
    )

    private val expectedIncludedCouponAndProduct2 = CouponAndProductEntity(
        couponId = couponDto.id,
        siteId = site.siteId,
        productId = couponDto.productIds!![1],
        isExcluded = false
    )

    private val expectedExcludedCouponAndProduct1 = CouponAndProductEntity(
        couponId = couponDto.id,
        siteId = site.siteId,
        productId = couponDto.excludedProductIds!![0],
        isExcluded = true
    )

    private val expectedExcludedCouponAndProduct2 = CouponAndProductEntity(
        couponId = couponDto.id,
        siteId = site.siteId,
        productId = couponDto.excludedProductIds!![1],
        isExcluded = true
    )

    private val expectedIncludedCouponAndCategory1 = CouponAndProductCategoryEntity(
        couponId = couponDto.id,
        siteId = site.siteId,
        productCategoryId = couponDto.productCategoryIds!![0],
        isExcluded = false
    )

    private val expectedIncludedCouponAndCategory2 = CouponAndProductCategoryEntity(
        couponId = couponDto.id,
        siteId = site.siteId,
        productCategoryId = couponDto.productCategoryIds!![1],
        isExcluded = false
    )

    private val expectedExcludedCouponAndCategory1 = CouponAndProductCategoryEntity(
        couponId = couponDto.id,
        siteId = site.siteId,
        productCategoryId = couponDto.excludedProductCategoryIds!![0],
        isExcluded = true
    )

    private val expectedExcludedCouponAndCategory2 = CouponAndProductCategoryEntity(
        couponId = couponDto.id,
        siteId = site.siteId,
        productCategoryId = couponDto.excludedProductCategoryIds!![1],
        isExcluded = true
    )

    @Before
    fun setUp() {
        couponStore = CouponStore(
            restClient,
            couponsDao,
            productsDao,
            productCategoriesDao,
            initCoroutineEngine(),
            productStore,
            database
        )

        val blockArg1 = argumentCaptor<Runnable>()
        whenever(database.runInTransaction(blockArg1.capture())).then {
            blockArg1.firstValue.run()
        }
    }

    @Test
    fun `Coupons is fetched with a correct result`() = test {
        whenever(restClient.fetchCoupons(
            site,
            CouponStore.DEFAULT_PAGE,
            CouponStore.DEFAULT_PAGE_SIZE
        )).thenReturn(WooPayload(arrayOf(couponDto)))

        val result = couponStore.fetchCoupons(site)

        assertThat(result).isEqualTo(WooResult(Unit))
    }

    @Test
    fun `Coupon is inserted in DB correctly`() = test {
        whenever(restClient.fetchCoupons(
            site,
            CouponStore.DEFAULT_PAGE,
            CouponStore.DEFAULT_PAGE_SIZE
        )).thenReturn(WooPayload(arrayOf(couponDto)))

        couponStore.fetchCoupons(site)

        verify(couponsDao).insertOrUpdateCoupon(expectedCoupon)
    }

    @Test
    fun `Coupon emails are inserted in DB correctly`() = test {
        whenever(restClient.fetchCoupons(
            site,
            CouponStore.DEFAULT_PAGE,
            CouponStore.DEFAULT_PAGE_SIZE
        )).thenReturn(WooPayload(arrayOf(couponDto)))

        couponStore.fetchCoupons(site)

        verify(couponsDao).insertOrUpdateCouponEmail(expectedEmail)
    }

    @Test
    fun `Coupon products are inserted in DB correctly`() = test {
        whenever(restClient.fetchCoupons(
            site,
            CouponStore.DEFAULT_PAGE,
            CouponStore.DEFAULT_PAGE_SIZE
        )).thenReturn(WooPayload(arrayOf(couponDto)))

        couponStore.fetchCoupons(site)

        verify(couponsDao).insertOrUpdateCouponAndProduct(expectedIncludedCouponAndProduct1)
        verify(couponsDao).insertOrUpdateCouponAndProduct(expectedIncludedCouponAndProduct2)
        verify(couponsDao).insertOrUpdateCouponAndProduct(expectedExcludedCouponAndProduct1)
        verify(couponsDao).insertOrUpdateCouponAndProduct(expectedExcludedCouponAndProduct2)
    }

    @Test
    fun `Coupon categories are inserted in DB correctly`() = test {
        whenever(restClient.fetchCoupons(
            site,
            CouponStore.DEFAULT_PAGE,
            CouponStore.DEFAULT_PAGE_SIZE
        )).thenReturn(WooPayload(arrayOf(couponDto)))

        couponStore.fetchCoupons(site)

        verify(couponsDao)
            .insertOrUpdateCouponAndProductCategory(expectedIncludedCouponAndCategory1)
        verify(couponsDao)
            .insertOrUpdateCouponAndProductCategory(expectedIncludedCouponAndCategory2)
        verify(couponsDao)
            .insertOrUpdateCouponAndProductCategory(expectedExcludedCouponAndCategory1)
        verify(couponsDao)
            .insertOrUpdateCouponAndProductCategory(expectedExcludedCouponAndCategory2)
    }

    @Test
    fun `Observing the DB changes returns the correct coupon data model`(): Unit = test {
        whenever(couponsDao.observeCoupons(site.siteId)).thenReturn(
            flowOf(listOf(CouponWithEmails(expectedCoupon, listOf(expectedEmail))))
        )

        // included products
        val includedProducts = listOf(includedProduct1, includedProduct2)
        whenever(productsDao.getCouponProducts(
            siteId = site.siteId,
            couponId = couponDto.id,
            areExcluded = false
        )).thenReturn(includedProducts)

        // excluded products
        val excludedProducts = listOf(excludedProduct1, excludedProduct2)
        whenever(productsDao.getCouponProducts(
            siteId = site.siteId,
            couponId = couponDto.id,
            areExcluded = true
        )).thenReturn(excludedProducts)

        // included categories
        val includedCategories = listOf(includedCategory1, includedCategory2)
        whenever(productCategoriesDao.getCouponProductCategories(
            siteId = site.siteId,
            couponId = couponDto.id,
            areExcluded = false
        )).thenReturn(includedCategories)

        // excluded categories
        val excludedCategories = listOf(excludedCategory1, excludedCategory2)
        whenever(productCategoriesDao.getCouponProductCategories(
            siteId = site.siteId,
            couponId = couponDto.id,
            areExcluded = true
        )).thenReturn(excludedCategories)

        val expectedDataModel = listOf(
            CouponDataModel(
                expectedCoupon,
                includedProducts,
                excludedProducts,
                includedCategories,
                excludedCategories,
                listOf(expectedEmail)
            )
        )

        val observedDataModel = couponStore.observeCoupons(site).first()

        assertThat(observedDataModel).isEqualTo(expectedDataModel)
    }

    @Test
    fun `Observing a specific coupon returns the correct coupon data model`(): Unit = test {
        whenever(couponsDao.observeCoupon(site.siteId, expectedCoupon.id)).thenReturn(
            flowOf(CouponWithEmails(expectedCoupon, listOf(expectedEmail)))
        )

        // included products
        val includedProducts = listOf(includedProduct1, includedProduct2)
        whenever(productsDao.getCouponProducts(
            siteId = site.siteId,
            couponId = couponDto.id,
            areExcluded = false
        )).thenReturn(includedProducts)

        // excluded products
        val excludedProducts = listOf(excludedProduct1, excludedProduct2)
        whenever(productsDao.getCouponProducts(
            siteId = site.siteId,
            couponId = couponDto.id,
            areExcluded = true
        )).thenReturn(excludedProducts)

        // included categories
        val includedCategories = listOf(includedCategory1, includedCategory2)
        whenever(productCategoriesDao.getCouponProductCategories(
            siteId = site.siteId,
            couponId = couponDto.id,
            areExcluded = false
        )).thenReturn(includedCategories)

        // excluded categories
        val excludedCategories = listOf(excludedCategory1, excludedCategory2)
        whenever(productCategoriesDao.getCouponProductCategories(
            siteId = site.siteId,
            couponId = couponDto.id,
            areExcluded = true
        )).thenReturn(excludedCategories)

        val expectedDataModel = CouponDataModel(
            expectedCoupon,
            includedProducts,
            excludedProducts,
            includedCategories,
            excludedCategories,
            listOf(expectedEmail)
        )

        val observedDataModel = couponStore.observeCoupon(site, expectedCoupon.id).first()

        assertThat(observedDataModel).isEqualTo(expectedDataModel)
    }
}
