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
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.persistence.entity.CouponAndProductEntity
import org.wordpress.android.fluxc.persistence.entity.ProductEntity

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ProductsDaoTest {
    private lateinit var couponsDao: CouponsDao
    private lateinit var productsDao: ProductsDao
    private lateinit var db: WCAndroidDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        productsDao = db.productsDao
        couponsDao = db.couponsDao
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `test product insert and update`(): Unit = runBlocking {
        // when
        var product = generateProductEntity(1)
        productsDao.insertOrUpdateProduct(product)

        // then
        var observedProduct = productsDao.getProductsByIds(product.siteId, listOf(product.id))
        assertThat(observedProduct.first()).isEqualTo(product)

        // when
        product = product.copy(name = "updated")
        productsDao.insertOrUpdateProduct(product)

        // then
        observedProduct = productsDao.getProductsByIds(product.siteId, listOf(product.id))
        assertThat(observedProduct.first()).isEqualTo(product)
    }

    @Test
    fun `test insert coupon products`(): Unit = runBlocking {
        // when
        val product1 = generateProductEntity(1)
        val product2 = generateProductEntity(2)
        val product3 = generateProductEntity(3)
        val product4 = generateProductEntity(4)
        productsDao.insertOrUpdateProduct(product1)
        productsDao.insertOrUpdateProduct(product2)
        productsDao.insertOrUpdateProduct(product3)
        productsDao.insertOrUpdateProduct(product4)

        val coupon = CouponsDaoTest.generateCouponEntity()
        couponsDao.insertOrUpdateCoupon(coupon)
        couponsDao.insertOrUpdateCouponAndProduct(
            CouponAndProductEntity(
                couponId = coupon.id,
                siteId = coupon.siteId,
                productId = product1.id,
                isExcluded = false
            )
        )
        couponsDao.insertOrUpdateCouponAndProduct(
            CouponAndProductEntity(
                couponId = coupon.id,
                siteId = coupon.siteId,
                productId = product2.id,
                isExcluded = false
            )
        )
        couponsDao.insertOrUpdateCouponAndProduct(
            CouponAndProductEntity(
                couponId = coupon.id,
                siteId = coupon.siteId,
                productId = product3.id,
                isExcluded = true
            )
        )
        couponsDao.insertOrUpdateCouponAndProduct(
            CouponAndProductEntity(
                couponId = coupon.id,
                siteId = coupon.siteId,
                productId = product4.id,
                isExcluded = true
            )
        )

        val includedProducts = productsDao.getCouponProducts(
            couponId = coupon.id,
            siteId = coupon.siteId,
            areExcluded = false
        )

        val excludedProducts = productsDao.getCouponProducts(
            couponId = coupon.id,
            siteId = coupon.siteId,
            areExcluded = true
        )

        // then
        assertThat(includedProducts).isEqualTo(listOf(product1, product2))
        assertThat(excludedProducts).isEqualTo(listOf(product3, product4))
    }

    companion object {
        fun generateProductEntity(
            remoteId: Long,
            type: String = "simple",
            name: String = "",
            virtual: Boolean = false,
            siteId: Long = 1,
            stockStatus: String = CoreProductStockStatus.IN_STOCK.value,
            status: String = "publish",
            stockQuantity: Double = 0.0,
            categories: String = ""
        ) = ProductEntity(
                id = remoteId,
                siteId = siteId,
                type = type,
                name = name,
                isVirtual = virtual,
                stockStatus = stockStatus,
                status = status,
                stockQuantity = stockQuantity
        )
    }
}
