package org.wordpress.android.fluxc.wc.product

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import kotlin.test.assertEquals

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class ProductSqlUtilsTest {
    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(
                        WCProductModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun testInsertOrUpdateProduct() {
        val productModel = ProductTestUtils.generateSampleProduct(40)
        val site = SiteModel().apply { id = productModel.localSiteId }

        // Test inserting product
        ProductSqlUtils.insertOrUpdateProduct(productModel)
        val storedProductsCount = ProductSqlUtils.getProductCountForSite(site)
        assertEquals(1, storedProductsCount)

        // Test updating order
        val storedProduct = ProductSqlUtils.getProductByRemoteId(site, productModel.remoteProductId)
        storedProduct?.apply {
            name = "Anitaa Test"
            virtual = true
        }
        storedProduct?.also {
            ProductSqlUtils.insertOrUpdateProduct(it)
        }

        val updatedProductsCount = ProductSqlUtils.getProductCountForSite(site)
        assertEquals(1, updatedProductsCount)

        val updatedProduct = ProductSqlUtils.getProductByRemoteId(site, productModel.remoteProductId)
        assertEquals(storedProduct?.id, updatedProduct?.id)
        assertEquals(storedProduct?.name, updatedProduct?.name)
        assertEquals(storedProduct?.virtual, updatedProduct?.virtual)
    }

    @Test
    fun testGetProductsForSite() {
        // insert products for one site
        val site1 = SiteModel().apply { id = 2 }
        val product1 = ProductTestUtils.generateSampleProduct(40, siteId = site1.id)
        ProductSqlUtils.insertOrUpdateProduct(product1)

        // verify that it is stored
        val storedProduct = ProductSqlUtils.getProductByRemoteId(site1, product1.remoteProductId)
        assertEquals(product1.id, storedProduct?.id)
        assertEquals(product1.name, storedProduct?.name)
        assertEquals(product1.virtual, storedProduct?.virtual)

        // insert products for another site
        val site2 = SiteModel().apply { id = 10 }
        val product2 = ProductTestUtils.generateSampleProduct(43, siteId = site2.id)
        ProductSqlUtils.insertOrUpdateProduct(product2)

        // verify that it is stored
        val storedProduct2 = ProductSqlUtils.getProductByRemoteId(site2, product2.remoteProductId)
        assertEquals(product2.id, storedProduct2?.id)
        assertEquals(product2.name, storedProduct2?.name)
        assertEquals(product2.virtual, storedProduct2?.virtual)

        // add another product for site 1
        val product3 = ProductTestUtils.generateSampleProduct(43, siteId = site1.id)
        ProductSqlUtils.insertOrUpdateProduct(product3)

        // verify that the site 2 product size is still the same
        val storedProductForSite2Count = ProductSqlUtils.getProductCountForSite(site2)
        assertEquals(1, storedProductForSite2Count)

        // verify that the site 1 product is increases by 1
        val storedProductForSite1Count = ProductSqlUtils.getProductCountForSite(site1)
        assertEquals(2, storedProductForSite1Count)
    }

    @Test
    fun testGetProductsForSiteAndProductIds() {
        val productIds = listOf<Long>(40, 41, 2)

        val product1 = ProductTestUtils.generateSampleProduct(40)
        val product2 = ProductTestUtils.generateSampleProduct(41)
        val product3 = ProductTestUtils.generateSampleProduct(42)

        ProductSqlUtils.insertOrUpdateProduct(product1)
        ProductSqlUtils.insertOrUpdateProduct(product2)
        ProductSqlUtils.insertOrUpdateProduct(product3)

        val site = SiteModel().apply { id = product1.localSiteId }
        val products = ProductSqlUtils.getProductsByRemoteIds(site, productIds)
        assertEquals(2, products.size)

        // insert products with the same productId but for a different site
        val differentSiteProduct1 = ProductTestUtils.generateSampleProduct(40, siteId = 10)
        val differentSiteProduct2 = ProductTestUtils.generateSampleProduct(41, siteId = 10)
        val differentSiteProduct3 = ProductTestUtils.generateSampleProduct(2, siteId = 10)

        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct1)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct2)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct3)

        // verify that the products for the first site is still 2
        assertEquals(2, ProductSqlUtils.getProductsByRemoteIds(site, productIds).size)

        // verify that the products for the second site is 3
        val site2 = SiteModel().apply { id = differentSiteProduct1.localSiteId }
        val differentSiteProducts = ProductSqlUtils.getProductsByRemoteIds(site2, productIds)
        assertEquals(3, differentSiteProducts.size)
    }
}
