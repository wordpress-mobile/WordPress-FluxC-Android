package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import java.io.IOException
import org.assertj.core.api.Assertions.assertThat
import org.wordpress.android.fluxc.persistence.entity.CouponAndProductCategoryEntity
import org.wordpress.android.fluxc.persistence.entity.ProductCategoryEntity

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ProductCategoriesDaoTest {
    private lateinit var couponsDao: CouponsDao
    private lateinit var productCategoriesDao: ProductCategoriesDao
    private lateinit var db: WCAndroidDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        productCategoriesDao = db.productCategoriesDao
        couponsDao = db.couponsDao
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `test product category insert and update`(): Unit = runBlocking {
        // when
        var category = generateProductCategoryEntity(1)
        productCategoriesDao.insertOrUpdateProductCategory(category)

        // then
        var observedProduct = productCategoriesDao
            .getProductCategoriesByIds(category.siteId, listOf(category.id))
        assertThat(observedProduct.first()).isEqualTo(category)

        // when
        category = category.copy(name = "updated")
        productCategoriesDao.insertOrUpdateProductCategory(category)

        // then
        observedProduct = productCategoriesDao
            .getProductCategoriesByIds(category.siteId, listOf(category.id))
        assertThat(observedProduct.first()).isEqualTo(category)
    }

    @Test
    fun `test insert coupon products`(): Unit = runBlocking {
        // when
        val cat1 = generateProductCategoryEntity(1)
        val cat2 = generateProductCategoryEntity(2)
        val cat3 = generateProductCategoryEntity(3)
        val cat4 = generateProductCategoryEntity(4)
        productCategoriesDao.insertOrUpdateProductCategory(cat1)
        productCategoriesDao.insertOrUpdateProductCategory(cat2)
        productCategoriesDao.insertOrUpdateProductCategory(cat3)
        productCategoriesDao.insertOrUpdateProductCategory(cat4)

        val coupon = CouponsDaoTest.generateCouponEntity()
        couponsDao.insertOrUpdateCoupon(coupon)
        couponsDao.insertOrUpdateCouponAndProductCategory(
            CouponAndProductCategoryEntity(
                couponId = coupon.id,
                siteId = coupon.siteId,
                productCategoryId = cat1.id,
                isExcluded = false
            )
        )
        couponsDao.insertOrUpdateCouponAndProductCategory(
            CouponAndProductCategoryEntity(
                couponId = coupon.id,
                siteId = coupon.siteId,
                productCategoryId = cat2.id,
                isExcluded = false
            )
        )
        couponsDao.insertOrUpdateCouponAndProductCategory(
            CouponAndProductCategoryEntity(
                couponId = coupon.id,
                siteId = coupon.siteId,
                productCategoryId = cat3.id,
                isExcluded = true
            )
        )
        couponsDao.insertOrUpdateCouponAndProductCategory(
            CouponAndProductCategoryEntity(
                couponId = coupon.id,
                siteId = coupon.siteId,
                productCategoryId = cat4.id,
                isExcluded = true
            )
        )

        val includedCategories = productCategoriesDao.getCouponProductCategories(
            couponId = coupon.id,
            siteId = coupon.siteId,
            areExcluded = false
        )

        val excludedCategories = productCategoriesDao.getCouponProductCategories(
            couponId = coupon.id,
            siteId = coupon.siteId,
            areExcluded = true
        )

        // then
        assertThat(includedCategories).isEqualTo(listOf(cat1, cat2))
        assertThat(excludedCategories).isEqualTo(listOf(cat3, cat4))
    }

    companion object {
        fun generateProductCategoryEntity(
            remoteId: Long,
            parentId: Long? = null,
            name: String = "",
            slug: String = "",
            siteId: Long = 1,
        ) = ProductCategoryEntity(
            id = remoteId,
            siteId = siteId,
            parentId = parentId,
            name = name,
            slug = slug
        )
    }
}
