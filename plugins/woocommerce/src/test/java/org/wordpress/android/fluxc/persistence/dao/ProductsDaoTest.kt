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
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.persistence.entity.ProductEntity

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ProductsDaoTest {
    private lateinit var couponsDao: CouponsDao
    private lateinit var productsDao: ProductsDao
    private lateinit var db: WCAndroidDatabase

    private val site = SiteModel().apply { siteId = SITE_ID }

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

    companion object {
        const val SITE_ID = 1L

        fun generateProductEntity(
            remoteId: Long,
            type: String = "simple",
            name: String = "",
            virtual: Boolean = false,
            siteId: Long = SITE_ID,
            stockStatus: String = CoreProductStockStatus.IN_STOCK.value,
            status: String = "publish",
            stockQuantity: Double = 0.0
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
