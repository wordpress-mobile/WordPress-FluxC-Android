package org.wordpress.android.fluxc.mocked

import com.google.gson.JsonArray
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.Subscribe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.WCProductAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.model.WCProductFileModel
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType
import org.wordpress.android.fluxc.store.WCProductStore.RemoteAddProductCategoryResponsePayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteAddProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteAddProductTagsResponsePayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteDeleteProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductCategoriesPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductListPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductShippingClassListPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductShippingClassPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductSkuAvailabilityPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductTagsPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteSearchProductsPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateProductImagesPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateProductPayload
import org.wordpress.android.fluxc.utils.DateUtils
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates.notNull

class MockedStack_WCProductsTest : MockedStack_Base() {
    @Inject internal lateinit var productRestClient: ProductRestClient
    @Inject internal lateinit var siteSqlUtils: SiteSqlUtils
    @Inject internal lateinit var dispatcher: Dispatcher

    @Inject internal lateinit var interceptor: ResponseMockingInterceptor

    private var lastAction: Action<*>? = null
    private var countDownLatch: CountDownLatch by notNull()

    private val remoteProductId = 1537L
    private val remoteShippingClassId = 34L
    private val searchQuery = "test"

    private val siteModel = SiteModel().apply {
        origin = SiteModel.ORIGIN_WPCOM_REST
        email = "test@example.org"
        name = "Test Site"
        siteId = 24
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mMockedNetworkAppComponent.inject(this)
        dispatcher.register(this)
        lastAction = null

        // Insert the site into the db so it's available later for product
        // reviews
        siteSqlUtils.insertOrUpdateSite(siteModel)
    }

    @Test
    fun testFetchSingleProductSuccess() = runBlocking {
        interceptor.respondWith("wc-fetch-product-response-success.json")
        val payload = productRestClient.fetchSingleProduct(siteModel, remoteProductId)

        with(payload) {
            assertNull(error)
            assertEquals(remoteProductId, productWithMetaData.product.remoteProductId)
            assertEquals(productWithMetaData.product.getCategoryList().size, 2)
            assertEquals(productWithMetaData.product.getTagList().size, 2)
            assertEquals(productWithMetaData.product.getImageListOrEmpty().size, 2)
            assertNotNull(productWithMetaData.product.getFirstImageUrl())
            assertEquals(productWithMetaData.product.getAttributeList().size, 2)
            assertEquals(productWithMetaData.product.getAttributeList().get(0).options.size, 3)
            assertEquals(
                productWithMetaData.product.getAttributeList().get(0).getCommaSeparatedOptions(),
                "Small, Medium, Large"
            )
            assertEquals(productWithMetaData.product.getVariationIdList().size, 2)
            assertEquals(productWithMetaData.product.getDownloadableFiles().size, 1)
            assertEquals(productWithMetaData.product.downloadExpiry, 10)
            assertEquals(productWithMetaData.product.downloadLimit, 123123124124)
        }

        // save the product to the db
        assertEquals(ProductSqlUtils.insertOrUpdateProduct(payload.productWithMetaData.product), 1)

        // now verify the db stored the product correctly
        val productFromDb = ProductSqlUtils.getProductByRemoteId(siteModel, remoteProductId)
        assertNotNull(productFromDb)
        productFromDb?.let { product ->
            assertEquals(product.remoteProductId, remoteProductId)
            assertEquals(product.getCategoryList().size, 2)
            assertEquals(product.getTagList().size, 2)
            assertEquals(product.getImageListOrEmpty().size, 2)
            assertNotNull(product.getFirstImageUrl())
            assertEquals(product.getAttributeList().size, 2)
            assertEquals(product.getAttributeList().get(0).options.size, 3)
            assertEquals(product.getAttributeList().get(0).getCommaSeparatedOptions(), "Small, Medium, Large")
            assertEquals(product.getVariationIdList().size, 2)
            assertEquals(product.getDownloadableFiles().size, 1)
            assertEquals(product.downloadExpiry, 10)
            assertEquals(product.downloadLimit, 123123124124)
        }
        Unit
    }

    @Test
    fun testFetchSingleProductError() = runBlocking {
        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        val payload = productRestClient.fetchSingleProduct(siteModel, remoteProductId)

        assertNotNull(payload.error)
    }

    @Test
    fun testFetchInvalidProductIdError() = runBlocking {
        interceptor.respondWithError("wc-fetch-invalid-product-id.json")
        val payload = productRestClient.fetchSingleProduct(siteModel, remoteProductId)

        assertNotNull(payload.error)
        assertEquals(payload.error.type, ProductErrorType.INVALID_PRODUCT_ID)
    }

    /**
     * ParentId used to be an integer. This test verifies the app correctly parses even long values.
     */
    @Test
    fun testFetchParentProductIdExceedingInteger() = runBlocking {
        interceptor.respondWith("wc-fetch-parent-product-id-exceeding-integer.json")
        val payload = productRestClient.fetchSingleProduct(siteModel, remoteProductId)

        assertEquals(5060147738939L, payload.productWithMetaData.product.parentId)
    }

    @Test
    fun testFetchSingleProductManageStock() = runBlocking {
        // check that a product's manage stock field is correctly set to true
        interceptor.respondWith("wc-fetch-product-response-manage-stock-true.json")
        val payloadTrue = productRestClient.fetchSingleProduct(siteModel, remoteProductId)
        assertTrue(payloadTrue.productWithMetaData.product.manageStock)

        // check that a product's manage stock field is correctly set to false
        interceptor.respondWith("wc-fetch-product-response-manage-stock-false.json")
        val payloadFalse = productRestClient.fetchSingleProduct(siteModel, remoteProductId)
        assertFalse(payloadFalse.productWithMetaData.product.manageStock)

        // check that a product's manage stock field is correctly set to true when response contains
        // "parent" rather than true/false (this is for product variations who inherit the parent's
        // manage stock)
        interceptor.respondWith("wc-fetch-product-response-manage-stock-parent.json")
        val payloadParent = productRestClient.fetchSingleProduct(siteModel, remoteProductId)
        assertTrue(payloadParent.productWithMetaData.product.manageStock)
    }

    @Test
    fun testFetchProductsSuccess() {
        interceptor.respondWith("wc-fetch-products-response-success.json")
        productRestClient.fetchProducts(siteModel)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_PRODUCTS, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductListPayload
        with(payload) {
            assertNull(error)
            assertNotNull(productsWithMetaData)
            assertEquals(productsWithMetaData.size, 4)
            assertNull(productsWithMetaData[0].product.getFirstImageUrl())

            // verify that response as json array in product response is handled correctly
            assertEquals("10,11,12", productsWithMetaData[0].product.getGroupedProductIdList().joinToString(","))
            assertEquals("10,11,12", productsWithMetaData[0].product.getUpsellProductIdList().joinToString(","))
            assertEquals("10,11,12", productsWithMetaData[0].product.getCrossSellProductIdList().joinToString(","))
            assertEquals(3, productsWithMetaData[0].product.getVariationIdList().size)

            // verify that response as json object in product response is handled correctly
            assertEquals("10,11,12", productsWithMetaData[1].product.getGroupedProductIdList().joinToString(","))
            assertEquals("10,11,12", productsWithMetaData[1].product.getUpsellProductIdList().joinToString(","))
            assertEquals("10,11,12", productsWithMetaData[1].product.getCrossSellProductIdList().joinToString(","))
            assertEquals(3, productsWithMetaData[1].product.getVariationIdList().size)

            // verify that response as null in product response is handled correctly
            assertEquals(0, productsWithMetaData[2].product.getGroupedProductIdList().size)
            assertEquals(0, productsWithMetaData[2].product.getUpsellProductIdList().size)
            assertEquals(0, productsWithMetaData[2].product.getCrossSellProductIdList().size)
            assertEquals(0, productsWithMetaData[2].product.getVariationIdList().size)
            assertEquals(0, productsWithMetaData[2].product.getCategoryList().size)

            // verify that variations are handled correctly when returned as array of objects
            assertEquals(2, productsWithMetaData[3].product.getVariationIdList().size)
            assertEquals(16892, productsWithMetaData[3].product.getVariationIdList()[0])
            assertEquals(16893, productsWithMetaData[3].product.getVariationIdList()[1])
        }

        // delete all products then insert these into the store
        ProductSqlUtils.deleteProductsForSite(siteModel)
        assertEquals(ProductSqlUtils.insertOrUpdateProducts(payload.productsWithMetaData.map { it.product }), 4)

        // now verify the db stored the products correctly
        val productsFromDb = ProductSqlUtils.getProductsForSite(siteModel)
        assertNotNull(productsFromDb)
        assertEquals(productsFromDb.size, 4)

        // verify that the products are correctly sorted
        assertEquals("Product 1", productsFromDb[0].name)
        assertEquals("Product 2", productsFromDb[1].name)
        assertEquals("Product 3", productsFromDb[2].name)
        assertEquals("Product 4", productsFromDb[3].name)
    }

    @Test
    fun testFetchProductsError() {
        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        productRestClient.fetchProducts(siteModel)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_PRODUCTS, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductListPayload
        assertNotNull(payload.error)
    }

    @Test
    fun testSearchProductsSuccess() {
        interceptor.respondWith("wc-fetch-products-response-success.json")
        productRestClient.searchProducts(
            site = siteModel,
            searchQuery = searchQuery
        )

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.SEARCHED_PRODUCTS, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteSearchProductsPayload
        assertNull(payload.error)
        assertEquals(payload.searchQuery, searchQuery)
        assertEquals(payload.skuSearchOptions, WCProductStore.SkuSearchOptions.Disabled)
    }

    @Test
    fun testSearchProductsBySkuSuccess() {
        interceptor.respondWith("wc-fetch-products-response-success.json")
        productRestClient.searchProducts(
            site = siteModel,
            searchQuery = searchQuery,
            skuSearchOptions = WCProductStore.SkuSearchOptions.PartialMatch
        )

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.SEARCHED_PRODUCTS, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteSearchProductsPayload
        assertNull(payload.error)
        assertEquals(payload.searchQuery, searchQuery)
        assertEquals(payload.skuSearchOptions, WCProductStore.SkuSearchOptions.PartialMatch)
    }

    @Test
    fun testSearchOrdersError() {
        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        productRestClient.searchProducts(siteModel, searchQuery)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.SEARCHED_PRODUCTS, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteSearchProductsPayload
        assertNotNull(payload.error)
        assertEquals(payload.searchQuery, searchQuery)
    }

    @Test
    fun testFetchProductSkuAvailabilitySuccess() {
        interceptor.respondWith("wc-fetch-products-response-success.json")
        productRestClient.fetchProductSkuAvailability(siteModel, "woo-hoodie123456")

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_PRODUCT_SKU_AVAILABILITY, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductSkuAvailabilityPayload
        with(payload) {
            assertNull(error)
            assertEquals(siteModel, site)
            assertFalse(available)
        }
    }

    @Test
    fun testFetchProductSkuAvailabilityError() {
        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        productRestClient.fetchProductSkuAvailability(siteModel, "woo-hoodie")

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_PRODUCT_SKU_AVAILABILITY, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductSkuAvailabilityPayload
        with(payload) {
            assertNotNull(error)
            assertNotNull(sku)
            assertTrue(available)
        }
    }

    @Test
    fun testFetchProductVariationsSuccess() = runBlocking {
        interceptor.respondWith("wc-fetch-product-variations-response-success.json")
        val result = productRestClient.fetchProductVariations(siteModel, remoteProductId)

        assertNull(result.error)
        assertEquals(remoteProductId, result.remoteProductId)
        assertEquals(3, result.variations.size)
        assertEquals("null", result.variations[0].image)
        assertNotNull(result.variations[1].image)

        // save the variation to the db
        assertEquals(ProductSqlUtils.insertOrUpdateProductVariations(result.variations), 3)

        // now delete all variations for this product and save again
        ProductSqlUtils.deleteVariationsForProduct(siteModel, remoteProductId)
        assertEquals(ProductSqlUtils.insertOrUpdateProductVariations(result.variations), 3)

        // now verify the db stored the variation correctly
        val dbVariations = ProductSqlUtils.getVariationsForProduct(siteModel, remoteProductId)
        assertEquals(dbVariations.size, 3)
        with(dbVariations.first()) {
            assertEquals(this.remoteProductId, remoteProductId)
            assertEquals(this.localSiteId, siteModel.id)

            // verify that the variant with the first date created is fetched first
            assertEquals(this.dateCreated, "2019-03-16T05:14:57")
        }
    }

    @Test
    fun testFetchProductVariationsWithSyncRequestSuccess() = runBlocking {
        interceptor.respondWith("wc-fetch-product-variations-response-success.json")
        val result = productRestClient.fetchProductVariationsWithSyncRequest(
            siteModel,
            remoteProductId,
            offset = 0,
            pageSize = WCProductStore.DEFAULT_PRODUCT_VARIATIONS_PAGE_SIZE
        )

        assertNull(result.error)

        val variations = result.result!!
        variations.forEach {
            assertEquals(remoteProductId, it.remoteProductId)
        }
        assertEquals(3, variations.size)
        assertEquals("null", variations[0].image)
        assertNotNull(variations[1].image)

        // save the variation to the db
        assertEquals(ProductSqlUtils.insertOrUpdateProductVariations(variations), 3)

        // now delete all variations for this product and save again
        ProductSqlUtils.deleteVariationsForProduct(siteModel, remoteProductId)
        assertEquals(ProductSqlUtils.insertOrUpdateProductVariations(variations), 3)

        // now verify the db stored the variation correctly
        val dbVariations = ProductSqlUtils.getVariationsForProduct(siteModel, remoteProductId)
        assertEquals(dbVariations.size, 3)
        with(dbVariations.first()) {
            assertEquals(this.remoteProductId, remoteProductId)
            assertEquals(this.localSiteId, siteModel.id)

            // verify that the variant with the first date created is fetched first
            assertEquals(this.dateCreated, "2019-03-16T05:14:57")
        }
    }

    @Test
    fun testFetchProductVariationsError() = runBlocking {
        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        val result = productRestClient.fetchProductVariations(siteModel, remoteProductId)

        assertNotNull(result.error)
    }

    @Test
    fun testFetchProductShippingClassesSuccess() {
        interceptor.respondWith("wc-fetch-product-shipping-classes-response-success.json")
        productRestClient.fetchProductShippingClassList(siteModel)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_PRODUCT_SHIPPING_CLASS_LIST, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductShippingClassListPayload
        assertNull(payload.error)
        assertEquals(payload.shippingClassList.size, 2)
        assertEquals(payload.shippingClassList[0].remoteShippingClassId, 34)
        assertEquals(payload.shippingClassList[0].name, "example1")
        assertEquals(payload.shippingClassList[0].slug, "example-1")
        assertEquals(payload.shippingClassList[0].description, "Testing shipping class")

        // save the shipping class list to the db
        assertEquals(ProductSqlUtils.insertOrUpdateProductShippingClassList(payload.shippingClassList), 2)

        // now delete all shipping class list for this site and save again
        ProductSqlUtils.deleteProductShippingClassListForSite(siteModel)
        assertEquals(ProductSqlUtils.insertOrUpdateProductShippingClassList(payload.shippingClassList), 2)

        // now verify the db stored the variation correctly
        val dbShippingClassList = ProductSqlUtils.getProductShippingClassListForSite(
                siteModel.id
        )
        assertEquals(dbShippingClassList.size, 2)
        with(dbShippingClassList.first()) {
            assertEquals(this.remoteShippingClassId, remoteShippingClassId)
            assertEquals(this.localSiteId, siteModel.id)
        }
    }

    @Test
    fun testFetchProductShippingClassesError() {
        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        productRestClient.fetchProductShippingClassList(siteModel)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_PRODUCT_SHIPPING_CLASS_LIST, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductShippingClassListPayload
        assertNotNull(payload.error)
    }

    @Test
    fun testFetchSingleProductShippingClassSuccess() {
        interceptor.respondWith("wc-fetch-product-shipping-class-response-success.json")
        productRestClient.fetchSingleProductShippingClass(siteModel, remoteShippingClassId)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_SINGLE_PRODUCT_SHIPPING_CLASS, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductShippingClassPayload
        with(payload) {
            assertNull(error)
            assertEquals(remoteShippingClassId, productShippingClassModel.remoteShippingClassId)
            assertEquals(siteModel.id, productShippingClassModel.localSiteId)
        }

        // save the product shipping class to the db
        assertEquals(ProductSqlUtils.insertOrUpdateProductShippingClass(payload.productShippingClassModel), 1)

        // now verify the db stored the product shipping class correctly
        val productShippingClassFromDb = ProductSqlUtils.getProductShippingClassByRemoteId(
                remoteShippingClassId, siteModel.id
        )
        assertNotNull(productShippingClassFromDb)
        productShippingClassFromDb?.let { productShippingClass ->
            assertEquals(productShippingClass.remoteShippingClassId, remoteShippingClassId)
            assertEquals(productShippingClass.localSiteId, siteModel.id)
            assertEquals(productShippingClass.name, "example1")
            assertEquals(productShippingClass.slug, "example-1")
        }
    }

    @Test
    fun testFetchSingleProductShippingClassError() {
        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        productRestClient.fetchSingleProductShippingClass(siteModel, remoteShippingClassId)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_SINGLE_PRODUCT_SHIPPING_CLASS, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductShippingClassPayload
        assertNotNull(payload.error)
    }

    @Test
    fun testFetchProductReviewsSuccess() = runBlocking {
        interceptor.respondWith("wc-fetch-product-reviews-response-success.json")
        val payload = productRestClient.fetchProductReviews(siteModel, 0)

        assertFalse(payload.isError)
        assertEquals(siteModel.id, payload.site.id)
        assertEquals(25, payload.reviews.size)
        assertNull(payload.filterProductIds)
        assertNull(payload.filterByStatus)
        assertTrue(payload.canLoadMore)

        // Save product reviews to the database
        assertEquals(25, ProductSqlUtils.insertOrUpdateProductReviews(payload.reviews))
        assertEquals(
                5,
                ProductSqlUtils.getProductReviewsForProductAndSiteId(siteModel.id, 22).size
        )
    }

    @Test
    fun testFetchProductReviewsFailed() = runBlocking {
        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        val payload = productRestClient.fetchProductReviews(siteModel, 0)

        assertTrue(payload.isError)
        assertEquals(ProductErrorType.GENERIC_ERROR, payload.error.type)
    }

    @Test
    fun testFetchProductReviewByReviewIdSuccess() = runBlocking {
        interceptor.respondWith("wc-fetch-product-review-response-success.json")
        val result = productRestClient.fetchProductReviewById(siteModel, 5499)

        // Verify payload and product review properties
        assertFalse(result.isError)
        assertEquals(siteModel.id, result.site.id)
        result.productReview?.let {
            with(it) {
                assertEquals(5499, remoteProductReviewId)
                assertEquals("2019-07-09T15:48:07Z", dateCreated)
                assertEquals(18, remoteProductId)
                assertEquals("approved", status)
                assertEquals("Johnny", reviewerName)
                assertEquals("johnny@gmail.com", reviewerEmail)
                assertEquals("<p>What a lovely cap!</p>\n", review)
                assertEquals(4, rating)
                assertEquals(false, verified)
                assertEquals(3, reviewerAvatarUrlBySize.size)
            }
        }
        Unit
    }

    @Test
    fun testFetchProductReviewByReviewIdFailed() = runBlocking {
        interceptor.respondWithError("wc-product-review-response-failure-invalid-id.json")
        val result = productRestClient.fetchProductReviewById(siteModel, 5499)

        assertTrue(result.isError)
        assertEquals(ProductErrorType.INVALID_REVIEW_ID, result.error.type)
    }

    @Test
    fun testUpdateProductReviewStatusSuccess() {
        runBlocking {
            interceptor.respondWith("wc-update-product-review-response-success.json")
            val result = productRestClient.updateProductReviewStatus(siteModel, 0, "spam")

            assertFalse(result.isError)
            result.result?.let {
                with(it) {
                    assertEquals(5499, remoteProductReviewId)
                    assertEquals("2019-07-09T15:48:07Z", dateCreated)
                    assertEquals(18, remoteProductId)
                    assertEquals("spam", status)
                    assertEquals("Johnny", reviewerName)
                    assertEquals("johnny@gmail.com", reviewerEmail)
                    assertEquals("<p>What a lovely cap!</p>\n", review)
                    assertEquals(4, rating)
                    assertEquals(false, verified)
                    assertEquals(3, reviewerAvatarUrlBySize.size)
                }
            }
        }
    }

    @Test
    fun testUpdateProductReviewStatusFailed() {
        runBlocking {
            interceptor.respondWithError("wc-response-failure-invalid-param.json")
            val result = productRestClient.updateProductReviewStatus(siteModel, 0, "spam")

            assertTrue(result.isError)
            assertEquals(WooErrorType.INVALID_ID, result.error.type)
        }
    }

    private fun generateTestImageList(): List<WCProductImageModel> {
        val imageList = ArrayList<WCProductImageModel>()
        with(WCProductImageModel(1)) {
            dateCreated = DateUtils.getCurrentDateString()
            alt = ""
            src = ""
            name = ""
            imageList.add(this)
        }
        with(WCProductImageModel(2)) {
            dateCreated = DateUtils.getCurrentDateString()
            alt = ""
            src = ""
            name = ""
            imageList.add(this)
        }
        return imageList
    }

    private fun generateTestImageListJsonString(): String {
        val jsonImages = JsonArray()
        for (image in generateTestImageList()) {
            jsonImages.add(image.toJson())
        }
        return jsonImages.toString()
    }

    private fun generateTestProduct(): WCProductModel {
        return WCProductModel(1).also {
            it.localSiteId = siteModel.id
            it.remoteProductId = remoteProductId
            it.name = "Product name"
            it.sku = "34343-343776"
        }
    }

    private fun generateSampleVariation(): WCProductVariationModel {
        return WCProductVariationModel().apply {
            remoteProductId = 1
            remoteVariationId = 181
            localSiteId = siteModel.id
        }
    }

    private fun generateTestFileListAsJsonString(): String {
        val json = WCProductFileModel(
                name = "Woo Single",
                url = "http://demo.woothemes.com/woocommerce/wp-content/uploads/sites/56/2013/06/cd_4_angle.jpg"
        )
                .toJson()

        return JsonArray()
                .apply { add(json) }
                .toString()
    }

    @Test
    fun testUpdateProductImagesSuccess() {
        interceptor.respondWith("wc-fetch-product-response-success.json")

        productRestClient.updateProductImages(siteModel, remoteProductId, generateTestImageList())

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.UPDATED_PRODUCT_IMAGES, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteUpdateProductImagesPayload
        with(payload) {
            assertNull(error)
            assertEquals(remoteProductId, productWithMetaData.product.remoteProductId)
            assertEquals(productWithMetaData.product.getImageListOrEmpty().size, 2)
        }
    }

    @Test
    fun testUpdateProductImagesFailed() {
        interceptor.respondWithError("wc-response-failure-invalid-param.json")
        productRestClient.updateProductImages(siteModel, remoteProductId, generateTestImageList())

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.UPDATED_PRODUCT_IMAGES, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteUpdateProductImagesPayload
        assertTrue(payload.isError)
    }

    @Test
    fun testDeleteProductVariationImageSuccess() = runBlocking {
        interceptor.respondWith("wc-delete-product-variation-image-success.json")

        val testVariation = generateSampleVariation()
        val updateVariation = testVariation.copy().apply {
            image = "{id:0}"
        }
        val payload = productRestClient.updateVariation(siteModel, testVariation, updateVariation)
        with(payload) {
            assertNull(error)
            assertNotNull(variation.image)
            assertEquals(variation.getImageModel()?.id, 2183L) // image will be the parent product image.
        }
    }

    @Test
    fun testDeleteProductVariationImageError() = runBlocking {
        interceptor.respondWithError("wc-delete-product-variation-image-failure.json", 400)

        val testVariation = generateSampleVariation()
        val updateVariation = testVariation.copy().apply {
            image = "{id:0}"
        }
        val payload = productRestClient.updateVariation(siteModel, testVariation, updateVariation)

        with(payload) {
            assertNotNull("Error should not be null", error)
            assertEquals(
                    "Error code should be woocommerce_variation_invalid_image_id",
                    ProductErrorType.INVALID_VARIATION_IMAGE_ID,
                    error.type)
        }
    }

    @Test
    fun testUpdateProductSuccess() {
        interceptor.respondWith("wc-fetch-product-response-success.json")

        val testProduct = generateTestProduct()
        val updatedProduct = testProduct.copy().apply {
            name = testProduct.name
            sku = testProduct.sku
            description = "Testing product description update"
            virtual = true
            images = generateTestImageListJsonString()
            groupedProductIds = "[10, 11]"
            crossSellIds = "[1, 2, 3]"
            upsellIds = "[1, 2, 3, 4]"
            type = "simple"
            downloadable = true
            downloads = generateTestFileListAsJsonString()
            downloadExpiry = 10
            downloadLimit = 123123124124
        }
        productRestClient.updateProduct(siteModel, testProduct, updatedProduct)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.UPDATED_PRODUCT, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteUpdateProductPayload
        with(payload) {
            assertNull(error)
            assertEquals(remoteProductId, productWithMetaData.product.remoteProductId)
            assertEquals(updatedProduct.description, productWithMetaData.product.description)
            assertEquals(updatedProduct.name, productWithMetaData.product.name)
            assertEquals(updatedProduct.sku, productWithMetaData.product.sku)
            assertEquals(updatedProduct.virtual, productWithMetaData.product.virtual)
            assertEquals(updatedProduct.getImageListOrEmpty().size, 2)
            assertEquals(updatedProduct.getGroupedProductIdList().size, 2)
            assertEquals(updatedProduct.getCrossSellProductIdList().size, 3)
            assertEquals(updatedProduct.getUpsellProductIdList().size, 4)
            assertEquals(updatedProduct.type, productWithMetaData.product.type)
            assertEquals(updatedProduct.getDownloadableFiles().size, 1)
            assertEquals(updatedProduct.downloadExpiry, 10)
            assertEquals(updatedProduct.downloadLimit, 123123124124)
        }
    }

    @Test
    fun testUpdateProductFailed() {
        interceptor.respondWithError("wc-response-failure-invalid-param.json")
        val testProduct = generateTestProduct()
        val updatedProduct = testProduct.copy().apply {
            description = "Testing product description"
        }
        productRestClient.updateProduct(siteModel, testProduct, updatedProduct)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.UPDATED_PRODUCT, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteUpdateProductPayload
        assertTrue(payload.isError)
    }

    @Test
    fun testDeleteProductSuccess() {
        interceptor.respondWith("wc-fetch-product-response-success.json")
        productRestClient.deleteProduct(siteModel, remoteProductId)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.DELETED_PRODUCT, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteDeleteProductPayload
        assertNull(payload.error)
        assertEquals(remoteProductId, payload.remoteProductId)

        // now verify the db no longer contains the product
        val productFromDb = ProductSqlUtils.getProductByRemoteId(siteModel, remoteProductId)
        assertNull(productFromDb)
    }

    @Test
    fun testDeleteProductFailed() {
        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        productRestClient.deleteProduct(siteModel, remoteProductId)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.DELETED_PRODUCT, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteDeleteProductPayload
        assertNotNull(payload.error)
    }

    @Test
    fun testDeleteProductFromDb() {
        // add our test product to the db
        val productTest = generateTestProduct()
        var rowsAffected = ProductSqlUtils.insertOrUpdateProduct(productTest)
        assertEquals(rowsAffected, 1)

        // then delete it
        rowsAffected = ProductSqlUtils.deleteProduct(siteModel, remoteProductId)
        assertEquals(rowsAffected, 1)
    }

    @Test
    fun testDeleteProductImageFromDb() {
        // add our test product to the db
        val productTest = generateTestProduct()
        var rowsAffected = ProductSqlUtils.insertOrUpdateProduct(productTest)
        assertEquals(rowsAffected, 1)

        // update the product's images with our test list
        rowsAffected = ProductSqlUtils.updateProductImages(productTest, generateTestImageList())
        assertEquals(rowsAffected, 1)

        // make sure two images are attached to the product
        val productBefore = ProductSqlUtils.getProductByRemoteId(siteModel, remoteProductId)
        assertNotNull(productBefore)
        assertEquals(productBefore!!.getImageListOrEmpty().size, 2)

        // remove one of the images
        val didDelete = ProductSqlUtils.deleteProductImage(siteModel, remoteProductId, 1)
        assertTrue(didDelete)

        // now make sure only one image is attached to the product
        val productAfter = ProductSqlUtils.getProductByRemoteId(siteModel, remoteProductId)
        assertNotNull(productAfter)
        assertEquals(productAfter!!.getImageListOrEmpty().size, 1)
    }

    @Test
    fun testAddProductCategorySuccess() = runBlocking {
        interceptor.respondWith("wc-add-product-category-response-success.json")

        val productCategoryModel = WCProductCategoryModel().apply { name = "test12" }
        productRestClient.addProductCategory(siteModel, productCategoryModel)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.ADDED_PRODUCT_CATEGORY, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteAddProductCategoryResponsePayload
        assertFalse(payload.isError)
        assertEquals(siteModel.id, payload.site.id)
        assertEquals(productCategoryModel.name, payload.category?.name)

        // Save product categories to the database
        assertEquals(1, ProductSqlUtils.insertOrUpdateProductCategory(payload.category!!))
        assertEquals(1, ProductSqlUtils.getProductCategoriesForSite(siteModel).size)
    }

    @Test
    fun testFetchProductCategoriesSuccess() {
        interceptor.respondWith("wc-fetch-all-product-categories-response-success.json")
        productRestClient.fetchProductCategories(siteModel)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_PRODUCT_CATEGORIES, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductCategoriesPayload
        assertFalse(payload.isError)
        assertEquals(siteModel.id, payload.site.id)
        assertEquals(7, payload.categories.size)
        assertFalse(payload.loadedMore)
        assertFalse(payload.canLoadMore)

        // Save product categories to the database
        assertEquals(7, ProductSqlUtils.insertOrUpdateProductCategories(payload.categories))
        assertEquals(
                7,
                ProductSqlUtils.getProductCategoriesForSite(siteModel).size
        )
    }

    @Test
    fun testFetchProductCategoriesFailed() {
        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        productRestClient.fetchProductCategories(siteModel)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_PRODUCT_CATEGORIES, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductCategoriesPayload
        assertTrue(payload.isError)
        assertEquals(ProductErrorType.GENERIC_ERROR, payload.error.type)
    }

    @Test
    fun testFetchProductTagsSuccess() {
        interceptor.respondWith("wc-fetch-product-tags-response-success.json")
        productRestClient.fetchProductTags(siteModel)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_PRODUCT_TAGS, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductTagsPayload
        assertNull(payload.error)
        assertEquals(payload.tags.size, 3)
        assertEquals(payload.tags[0].remoteTagId, 1)
        assertEquals(payload.tags[0].name, "awoo")
        assertEquals(payload.tags[0].slug, "awoo")
        assertEquals(payload.tags[0].description, "")

        // save the tags to the db
        assertEquals(ProductSqlUtils.insertOrUpdateProductTags(payload.tags), 3)

        // now delete all tags for this site and save again
        ProductSqlUtils.deleteProductTagsForSite(siteModel)
        assertEquals(ProductSqlUtils.insertOrUpdateProductTags(payload.tags), 3)

        // now verify the db stored the tags correctly
        val dbTags = ProductSqlUtils.getProductTagsForSite(siteModel.id)
        assertEquals(dbTags.size, 3)
        with(dbTags.first()) {
            assertEquals(this.remoteTagId, 1)
            assertEquals(this.localSiteId, siteModel.id)
        }
    }

    @Test
    fun testFetchProductTagsError() {
        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        productRestClient.fetchProductTags(siteModel)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_PRODUCT_TAGS, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductTagsPayload
        assertNotNull(payload.error)
    }

    @Test
    fun testAddProductTagsSuccess() {
        val tagNames = listOf("batchAdd1", "batchAdd6")
        interceptor.respondWith("wc-add-product-tags-response-success.json")
        productRestClient.addProductTags(siteModel, tagNames)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.ADDED_PRODUCT_TAGS, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteAddProductTagsResponsePayload
        assertNull(payload.error)
        assertEquals(2, payload.tags.size)
        assertEquals(payload.tags[0].name, "")
        assertEquals(payload.tags[1].name, "batchAdd6")

        // save the tags to the db
        payload.tags.map {
            if (it.name.isNotEmpty()) {
                assertEquals(1, ProductSqlUtils.insertOrUpdateProductTag(it))
            }
        }

        val savedTags = ProductSqlUtils.getProductTagsByNames(siteModel.id, tagNames)
        assertEquals(1, savedTags.size)
        assertEquals("batchAdd6", savedTags[0].name)
    }

    @Test
    fun testAddProductTagsError() {
        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        productRestClient.addProductTags(siteModel, listOf("test1", "test2"))

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.ADDED_PRODUCT_TAGS, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteAddProductTagsResponsePayload
        assertNotNull(payload.error)
    }

    @Test
    fun testAddProductSuccess() {
        interceptor.respondWith("wc-fetch-product-response-success.json")

        val testProduct = generateTestProduct()
        productRestClient.addProduct(siteModel, testProduct)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.ADDED_PRODUCT, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteAddProductPayload
        with(payload) {
            assertNull(error)
            assertEquals(remoteProductId, productWithMetaData.product.remoteProductId)
            assertEquals("simple", productWithMetaData.product.type)
            assertEquals("Testing product description update", productWithMetaData.product.description)
        }
    }

    @Test
    fun testAddProductFailed() {
        interceptor.respondWithError("wc-response-failure-invalid-param.json")
        val testProduct = generateTestProduct()
        productRestClient.addProduct(siteModel, testProduct)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.ADDED_PRODUCT, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteAddProductPayload
        assertTrue(payload.isError)
    }

    @Suppress("unused")
    @Subscribe
    fun onAction(action: Action<*>) {
        lastAction = action
        countDownLatch.countDown()
    }
}
