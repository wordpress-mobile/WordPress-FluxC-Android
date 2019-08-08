package org.wordpress.android.fluxc.wc.product

import com.nhaarman.mockitokotlin2.mock
import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCProductStore
import kotlin.test.assertEquals

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCProductStoreTest {
    private val productStore = WCProductStore(Dispatcher(), mock())

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(WCProductModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun testSimpleInsertionAndRetrieval() {
        val productModel = ProductTestUtils.generateSampleProduct(42)
        val site = SiteModel().apply { id = productModel.localSiteId }

        ProductSqlUtils.insertOrUpdateProduct(productModel)

        val storedProduct = productStore.getProductByRemoteId(site, productModel.remoteProductId)
        assertEquals(42, storedProduct?.remoteProductId)
        assertEquals(productModel, storedProduct)
    }

    @Test
    fun testGetProductsBySiteAndProductIds() {
        val productIds = listOf<Long>(30, 31, 2)

        val product1 = ProductTestUtils.generateSampleProduct(30)
        val product2 = ProductTestUtils.generateSampleProduct(31)
        val product3 = ProductTestUtils.generateSampleProduct(42)

        ProductSqlUtils.insertOrUpdateProduct(product1)
        ProductSqlUtils.insertOrUpdateProduct(product2)
        ProductSqlUtils.insertOrUpdateProduct(product3)

        val site = SiteModel().apply { id = product1.localSiteId }
        val products = productStore.getProductsByRemoteIds(site, productIds)
        assertEquals(2, products.size)

        // insert products with the same productId but for a different site
        val differentSiteProduct1 = ProductTestUtils.generateSampleProduct(10, siteId = 10)
        val differentSiteProduct2 = ProductTestUtils.generateSampleProduct(11, siteId = 10)
        val differentSiteProduct3 = ProductTestUtils.generateSampleProduct(2, siteId = 10)

        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct1)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct2)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct3)

        // verify that the products for the first site is still 2
        assertEquals(2, productStore.getProductsByRemoteIds(site, productIds).size)

        // verify that the products for the second site is 3
        val site2 = SiteModel().apply { id = differentSiteProduct1.localSiteId }
        val differentSiteProducts = productStore.getProductsByRemoteIds(site2, listOf(10, 11, 2))
        assertEquals(3, differentSiteProducts.size)
        assertEquals(differentSiteProduct1.remoteProductId, differentSiteProducts[0].remoteProductId)
        assertEquals(differentSiteProduct2.remoteProductId, differentSiteProducts[1].remoteProductId)
        assertEquals(differentSiteProduct3.remoteProductId, differentSiteProducts[2].remoteProductId)
    }
}
