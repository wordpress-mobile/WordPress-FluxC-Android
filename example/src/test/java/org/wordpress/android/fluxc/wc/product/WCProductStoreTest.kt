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
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.RemoteAddProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateVariationPayload
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCProductStoreTest {
    private val productStore = WCProductStore(
            Dispatcher(),
            mock(),
            addonsDao = mock(),
            logger = mock(),
            coroutineEngine = initCoroutineEngine()
    )

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(WCProductModel::class.java, WCProductVariationModel::class.java),
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

    @Test
    fun testUpdateProduct() {
        val productModel = ProductTestUtils.generateSampleProduct(42).apply {
            name = "test product"
            description = "test description"
        }
        val site = SiteModel().apply { id = productModel.localSiteId }
        ProductSqlUtils.insertOrUpdateProduct(productModel)

        // Simulate incoming action with updated product model
        val payload = RemoteUpdateProductPayload(site, productModel.apply {
            description = "Updated description"
        })
        productStore.onAction(WCProductActionBuilder.newUpdatedProductAction(payload))

        with(productStore.getProductByRemoteId(site, productModel.remoteProductId)) {
            // The version of the product model in the database should have the updated description
            assertEquals(productModel.description, this?.description)
            // Other fields should not be altered by the update
            assertEquals(productModel.name, this?.name)
        }
    }

    @Test
    fun testUpdateVariation() {
        val variationModel = ProductTestUtils.generateSampleVariation(42, 24).apply {
            description = "test description"
        }
        val site = SiteModel().apply { id = variationModel.localSiteId }
        ProductSqlUtils.insertOrUpdateProductVariation(variationModel)

        // Simulate incoming action with updated product model
        val payload = RemoteUpdateVariationPayload(site, variationModel.apply {
            description = "Updated description"
        })
        productStore.onAction(WCProductActionBuilder.newUpdatedVariationAction(payload))

        with(productStore.getVariationByRemoteId(
                site,
                variationModel.remoteProductId,
                variationModel.remoteVariationId
        )) {
            // The version of the product model in the database should have the updated description
            assertEquals(variationModel.description, this?.description)
            // Other fields should not be altered by the update
            assertEquals(variationModel.status, this?.status)
        }
    }

    @Test
    fun testVerifySkuExistsLocally() {
        val sku = "woo-cap"
        val productModel = ProductTestUtils.generateSampleProduct(42).apply {
            name = "test product"
            description = "test description"
            this.sku = sku
        }
        val site = SiteModel().apply { id = productModel.localSiteId }
        ProductSqlUtils.insertOrUpdateProduct(productModel)

        // verify if product with sku: woo-cap exists in local cache
        val skuAvailable = ProductSqlUtils.getProductExistsBySku(site, sku)
        assertTrue(skuAvailable)

        // verify if product with non existent sku returns false
        assertFalse(ProductSqlUtils.getProductExistsBySku(site, "woooo"))
    }

    @Test
    fun testGetProductsByFilterOptions() {
        val filterOptions = mapOf<ProductFilterOption, String>(
                ProductFilterOption.TYPE to "simple",
                ProductFilterOption.STOCK_STATUS to "instock",
                ProductFilterOption.STATUS to "publish",
                ProductFilterOption.CATEGORY to "1337"
        )
        val product1 = ProductTestUtils.generateSampleProduct(3)
        val product2 = ProductTestUtils.generateSampleProduct(
                31, type = "variable", status = "draft"
        )
        val product3 = ProductTestUtils.generateSampleProduct(
                42, stockStatus = "onbackorder", status = "pending"
                )

        val product4 = ProductTestUtils.generateSampleProduct(
                43, categories = "[{\"id\":1337,\"name\":\"Clothing\",\"slug\":\"clothing\"}]")

        ProductSqlUtils.insertOrUpdateProduct(product1)
        ProductSqlUtils.insertOrUpdateProduct(product2)
        ProductSqlUtils.insertOrUpdateProduct(product3)
        ProductSqlUtils.insertOrUpdateProduct(product4)

        val site = SiteModel().apply { id = product1.localSiteId }
        val products = productStore.getProductsByFilterOptions(site, filterOptions)
        assertEquals(1, products.size)

        // insert products with the same product options but for a different site
        val differentSiteProduct1 = ProductTestUtils.generateSampleProduct(10, siteId = 10)
        val differentSiteProduct2 = ProductTestUtils.generateSampleProduct(
                11, siteId = 10, type = "variable", status = "draft"
        )
        val differentSiteProduct3 = ProductTestUtils.generateSampleProduct(
                12, siteId = 10, stockStatus = "onbackorder", status = "pending"
        )
        val differentSiteProduct4 = ProductTestUtils.generateSampleProduct(
                13, siteId = 10, categories = "[{\"id\":1337,\"name\":\"Clothing\",\"slug\":\"clothing\"}]")

        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct1)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct2)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct3)
        ProductSqlUtils.insertOrUpdateProduct(differentSiteProduct4)

        // verify that the products for the first site is still 1
        assertEquals(1, productStore.getProductsByFilterOptions(site, filterOptions).size)

        // verify that the products for the second site is 3
        val site2 = SiteModel().apply { id = differentSiteProduct1.localSiteId }
        val filterOptions2 = mapOf(ProductFilterOption.STATUS to "draft")
        val differentSiteProducts = productStore.getProductsByFilterOptions(site2, filterOptions2)
        assertEquals(1, differentSiteProducts.size)
        assertEquals(differentSiteProduct2.status, differentSiteProducts[0].status)

        val filterOptions3 = mapOf(ProductFilterOption.STOCK_STATUS to "onbackorder")
        val differentProductFilters = productStore.getProductsByFilterOptions(site2, filterOptions3)
        assertEquals(1, differentProductFilters.size)
        assertEquals(differentSiteProduct3.stockStatus, differentProductFilters[0].stockStatus)

        val filterByCategory = mapOf(ProductFilterOption.CATEGORY to "1337")
        val productsFilteredByCategory = productStore.getProductsByFilterOptions(site2, filterByCategory)
        assertEquals(1, productsFilteredByCategory.size)
        assertTrue(productsFilteredByCategory[0].categories.contains("1337"))
    }

    @Test
    fun `test AddProduct Action triggered correctly`() {
        // given
        val productModel = ProductTestUtils.generateSampleProduct(remoteId = 0).apply {
            name = "test new product"
            description = "test new description"
        }
        val site = SiteModel().apply { id = productModel.localSiteId }

        // when
        ProductSqlUtils.insertOrUpdateProduct(productModel)
        val payload = RemoteAddProductPayload(site, productModel)
        productStore.onAction(WCProductActionBuilder.newAddedProductAction(payload))

        // then
        with(productStore.getProductByRemoteId(site, productModel.remoteProductId)) {
            assertNotNull(this)
            assertEquals(productModel.description, this?.description)
            assertEquals(productModel.name, this?.name)
        }
    }
}
