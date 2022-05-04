package org.wordpress.android.fluxc.wc.product

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.yarolegovich.wellsql.WellSql
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.BatchProductVariationsUpdateApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductVariationApiResponse
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.persistence.dao.ProductCategoriesDao
import org.wordpress.android.fluxc.persistence.dao.ProductsDao
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.BatchUpdateVariationsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductReviewPayload
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.RemoteAddProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductReviewPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateVariationPayload
import org.wordpress.android.fluxc.store.WCProductStore.UpdateVariationPayload
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.utils.ProductCategoriesDbHelper
import org.wordpress.android.fluxc.utils.ProductsDbHelper
import org.wordpress.android.fluxc.wc.product.ProductTestUtils.generateSampleVariations
import org.wordpress.android.fluxc.wc.utils.SiteTestUtils
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCProductStoreTest {
    private val productRestClient: ProductRestClient = mock()
    private val productsDao: ProductsDao = mock()
    private val productsDbHelper = ProductsDbHelper(productsDao)
    private val productCategoriesDao: ProductCategoriesDao = mock()
    private val productCategoriesDbHelper = ProductCategoriesDbHelper(productCategoriesDao)
    private val productStore = WCProductStore(
            Dispatcher(),
            productRestClient,
            addonsDao = mock(),
            logger = mock(),
            coroutineEngine = initCoroutineEngine(),
            productsDbHelper = productsDbHelper,
            productCategoriesDbHelper = productCategoriesDbHelper
    )

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(
                        WCProductModel::class.java,
                        WCProductVariationModel::class.java,
                        WCProductReviewModel::class.java,
                        SiteModel::class.java,
                        AccountModel::class.java
                ),
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
    fun testUpdateVariation() = runBlocking {
        val variationModel = ProductTestUtils.generateSampleVariation(42, 24).apply {
            description = "test description"
        }
        val site = SiteModel().apply { id = variationModel.localSiteId }
        whenever(productRestClient.updateVariation(site, null, variationModel))
            .thenReturn(RemoteUpdateVariationPayload(site, variationModel))

        productStore.updateVariation(UpdateVariationPayload(site, variationModel))

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
            assertEquals(productModel.description, this.description)
            assertEquals(productModel.name, this.name)
        }
    }

    @Test
    fun `given product review exists, when fetch product review, then local database updated`() = runBlocking {
        val site = SiteTestUtils.insertTestAccountAndSiteIntoDb()
        val productModel = ProductTestUtils.generateSampleProduct(remoteId = 1).apply {
            localSiteId = site.id
        }
        ProductSqlUtils.insertOrUpdateProduct(productModel)

        val reviewModel = ProductTestUtils.generateSampleProductReview(
                productModel.remoteProductId,
                123L,
                site.id
        )
        whenever(productRestClient.fetchProductReviewById(site, reviewModel.remoteProductReviewId))
                .thenReturn(RemoteProductReviewPayload(site, reviewModel))

        productStore.fetchSingleProductReview(FetchSingleProductReviewPayload(site, reviewModel.remoteProductReviewId))

        assertThat(productStore.getProductReviewByRemoteId(site.id, reviewModel.remoteProductReviewId)).isNotNull
        Unit
    }

    @Test
    fun `batch variations update should return positive result on successful backend request`() =
        runBlocking {
            // given
            val product = ProductTestUtils.generateSampleProduct(Random.nextLong())
            val site = SiteModel().apply { id = product.localSiteId }
            val variations = generateSampleVariations(
                number = 64,
                productId = product.remoteProductId,
                siteId = site.id
            )

            // when
            val variationsIds = variations.map { it.remoteVariationId }
            val variationsUpdatePayload = BatchUpdateVariationsPayload.Builder(site, product.remoteProductId, variationsIds).build()
            val response = BatchProductVariationsUpdateApiResponse().apply { updatedVariations = emptyList() }
            whenever(productRestClient.batchUpdateVariations(any(), any(), any(), any())) doReturn WooPayload(response)
            val result = productStore.batchUpdateVariations(variationsUpdatePayload)

            // then
            assertThat(result.isError).isFalse
            assertThat(result.model).isNotNull
            Unit
        }

    @Test
    fun `batch variations update should return negative result on failed backend request`() = runBlocking {
        // given
        val product = ProductTestUtils.generateSampleProduct(Random.nextLong())
        val site = SiteModel().apply { id = product.localSiteId }
        val variations = generateSampleVariations(
            number = 64,
            productId = product.remoteProductId,
            siteId = site.id
        )

        // when
        val variationsIds = variations.map { it.remoteVariationId }
        val variationsUpdatePayload =
            BatchUpdateVariationsPayload.Builder(site, product.remoteProductId, variationsIds).build()
        val errorResponse = WooError(GENERIC_ERROR, NETWORK_ERROR, "🔴")
        whenever(productRestClient.batchUpdateVariations(any(), any(), any(), any())) doReturn WooPayload(errorResponse)
        val result = productStore.batchUpdateVariations(variationsUpdatePayload)

        // then
        assertThat(result.isError).isTrue
        Unit
    }

    @Test
    fun `batch variations update should update variations locally after successful backend request`() =
        runBlocking {
            // given
            val product = ProductTestUtils.generateSampleProduct(Random.nextLong())
            ProductSqlUtils.insertOrUpdateProduct(product)
            val site = SiteTestUtils.insertTestAccountAndSiteIntoDb()
            val variations = generateSampleVariations(
                number = 64,
                productId = product.remoteProductId,
                siteId = site.id
            )
            ProductSqlUtils.insertOrUpdateProductVariations(variations)

            // when
            val variationsIds = variations.map { it.remoteVariationId }
            val newRegularPrice = "1.234 💰"
            val newSalePrice = "0.234 💰"
            val variationsUpdatePayload =
                BatchUpdateVariationsPayload.Builder(site, product.remoteProductId, variationsIds)
                    .regularPrice(newRegularPrice)
                    .salePrice(newSalePrice)
                    .build()
            val modifications = variationsUpdatePayload.modifiedProperties
            val variationsReturnedFromBackend = variations.map {
                ProductVariationApiResponse().apply {
                    id = it.remoteVariationId
                    regular_price = newRegularPrice
                    sale_price = newSalePrice
                }
            }
            val response = BatchProductVariationsUpdateApiResponse().apply { updatedVariations = variationsReturnedFromBackend }
            whenever(productRestClient.batchUpdateVariations(any(), any(), any(), any())) doReturn WooPayload(response)
            val result = productStore.batchUpdateVariations(variationsUpdatePayload)

            // then
            assertThat(result.isError).isFalse
            assertThat(result.model).isNotNull
            with(ProductSqlUtils.getVariationsForProduct(site, product.remoteProductId)) {
                forEach { variation ->
                    assertThat(variation.regularPrice).isEqualTo(newRegularPrice)
                    assertThat(variation.salePrice).isEqualTo(newSalePrice)
                }
            }
            Unit
        }

    @Test
    fun `batch variations update should not update variations locally after failed backend request`() =
        runBlocking {
            // given
            val product = ProductTestUtils.generateSampleProduct(Random.nextLong())
            ProductSqlUtils.insertOrUpdateProduct(product)
            val site = SiteTestUtils.insertTestAccountAndSiteIntoDb()
            val variations = generateSampleVariations(
                number = 64,
                productId = product.remoteProductId,
                siteId = site.id
            )
            ProductSqlUtils.insertOrUpdateProductVariations(variations)

            // when
            val variationsIds = variations.map { it.remoteVariationId }
            val newRegularPrice = "1.234 💰"
            val newSalePrice = "0.234 💰"
            val variationsUpdatePayload =
                BatchUpdateVariationsPayload.Builder(site, product.remoteProductId, variationsIds)
                    .regularPrice(newRegularPrice)
                    .salePrice(newSalePrice)
                    .build()
            val errorResponse = WooError(GENERIC_ERROR, NETWORK_ERROR, "🔴")
            whenever(productRestClient.batchUpdateVariations(any(), any(), any(), any())) doReturn WooPayload(errorResponse)
            val result = productStore.batchUpdateVariations(variationsUpdatePayload)

            // then
            assertThat(result.isError).isTrue
            with(ProductSqlUtils.getVariationsForProduct(site, product.remoteProductId)) {
                forEach { variation ->
                    assertThat(variation.regularPrice).isNotEqualTo(newRegularPrice)
                    assertThat(variation.salePrice).isNotEqualTo(newSalePrice)
                }
            }
            Unit
        }

    @Test
    fun `BatchUpdateVariationsPayload_Builder should produce correct variations modifications map`() {
        // given
        val product = ProductTestUtils.generateSampleProduct(Random.nextLong())
        val site = SiteModel().apply { id = 23 }
        val variations = generateSampleVariations(number = 64, productId = product.remoteProductId, siteId = site.id)
        val builder = BatchUpdateVariationsPayload.Builder(site, product.remoteProductId, variations.map { it.remoteVariationId })

        val modifiedRegularPrice = "11234.234"
        val modifiedSalePrice = "123,2.4"
        val modifiedStartOfSale = "tomorrow"
        val modifiedEndOfSale = "next week"
        val modifiedStartOfSaleGmt = "tomorrow"
        val modifiedEndOfSaleGmt = "next week"
        val modifiedStockQuantity = 1234
        val modifiedStockStatus = CoreProductStockStatus.IN_STOCK
        val modifiedWeight = "1234 kg"
        val modifiedDimensions = "10x12x10 cm"
        val modifiedShippingClassId = "1234"
        val modifiedShippingClassSlug = "DHL"

        // when
        builder.regularPrice(modifiedRegularPrice)
            .salePrice(modifiedSalePrice)
            .startOfSale(modifiedStartOfSale)
            .endOfSale(modifiedEndOfSale)
            .stockQuantity(modifiedStockQuantity)
            .stockStatus(modifiedStockStatus)
            .weight(modifiedWeight)
            .dimensions(modifiedDimensions)
            .shippingClassId(modifiedShippingClassId)
            .shippingClassSlug(modifiedShippingClassSlug)
        val payload = builder.build()

        // then
        with(payload.modifiedProperties) {
            assertThat(get("regular_price")).isEqualTo(modifiedRegularPrice)
            assertThat(get("sale_price")).isEqualTo(modifiedSalePrice)
            assertThat(get("date_on_sale_from")).isEqualTo(modifiedStartOfSale)
            assertThat(get("date_on_sale_to")).isEqualTo(modifiedEndOfSale)
            assertThat(get("date_on_sale_from_gmt")).isEqualTo(modifiedStartOfSaleGmt)
            assertThat(get("date_on_sale_to_gmt")).isEqualTo(modifiedEndOfSaleGmt)
            assertThat(get("stock_quantity")).isEqualTo(modifiedStockQuantity)
            assertThat(get("stock_status")).isEqualTo(modifiedStockStatus)
            assertThat(get("weight")).isEqualTo(modifiedWeight)
            assertThat(get("dimensions")).isEqualTo(modifiedDimensions)
            assertThat(get("shipping_class_id")).isEqualTo(modifiedShippingClassId)
            assertThat(get("shipping_class")).isEqualTo(modifiedShippingClassSlug)
        }
    }
}
