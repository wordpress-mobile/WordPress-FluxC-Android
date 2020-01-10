package org.wordpress.android.fluxc.mocked

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
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductReviewsResponsePayload
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductListPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductReviewPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductShippingClassListPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductSkuAvailabilityPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductVariationsPayload
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
    @Inject internal lateinit var dispatcher: Dispatcher

    @Inject internal lateinit var interceptor: ResponseMockingInterceptor

    private var lastAction: Action<*>? = null
    private var countDownLatch: CountDownLatch by notNull()

    private val remoteProductId = 1537L
    private val searchQuery = "test"

    private val siteModel = SiteModel().apply {
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
        SiteSqlUtils.insertOrUpdateSite(siteModel)
    }

    @Test
    fun testFetchSingleProductSuccess() {
        interceptor.respondWith("wc-fetch-product-response-success.json")
        productRestClient.fetchSingleProduct(siteModel, remoteProductId)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_SINGLE_PRODUCT, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductPayload
        with(payload) {
            assertNull(error)
            assertEquals(remoteProductId, product.remoteProductId)
            assertEquals(product.getCategories().size, 2)
            assertEquals(product.getTags().size, 2)
            assertEquals(product.getImages().size, 2)
            assertNotNull(product.getFirstImageUrl())
            assertEquals(product.getAttributes().size, 2)
            assertEquals(product.getAttributes().get(0).options.size, 3)
            assertEquals(product.getAttributes().get(0).getCommaSeparatedOptions(), "Small, Medium, Large")
            assertEquals(product.getNumVariations(), 2)
        }

        // save the product to the db
        assertEquals(ProductSqlUtils.insertOrUpdateProduct(payload.product), 1)

        // now verify the db stored the product correctly
        val productFromDb = ProductSqlUtils.getProductByRemoteId(siteModel, remoteProductId)
        assertNotNull(productFromDb)
        productFromDb?.let { product ->
            assertEquals(product.remoteProductId, remoteProductId)
            assertEquals(product.getCategories().size, 2)
            assertEquals(product.getTags().size, 2)
            assertEquals(product.getImages().size, 2)
            assertNotNull(product.getFirstImageUrl())
            assertEquals(product.getAttributes().size, 2)
            assertEquals(product.getAttributes().get(0).options.size, 3)
            assertEquals(product.getAttributes().get(0).getCommaSeparatedOptions(), "Small, Medium, Large")
            assertEquals(product.getNumVariations(), 2)
        }
    }

    @Test
    fun testFetchSingleProductError() {
        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        productRestClient.fetchSingleProduct(siteModel, remoteProductId)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_SINGLE_PRODUCT, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductPayload
        assertNotNull(payload.error)
    }

    @Test
    fun testFetchSingleProductManageStock() {
        // check that a product's manage stock field is correctly set to true
        interceptor.respondWith("wc-fetch-product-response-manage-stock-true.json")
        productRestClient.fetchSingleProduct(siteModel, remoteProductId)
        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
        val payloadTrue = lastAction!!.payload as RemoteProductPayload
        assertTrue(payloadTrue.product.manageStock)

        // check that a product's manage stock field is correctly set to false
        interceptor.respondWith("wc-fetch-product-response-manage-stock-false.json")
        productRestClient.fetchSingleProduct(siteModel, remoteProductId)
        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
        val payloadFalse = lastAction!!.payload as RemoteProductPayload
        assertFalse(payloadFalse.product.manageStock)

        // check that a product's manage stock field is correctly set to true when response contains
        // "parent" rather than true/false (this is for product variations who inherit the parent's
        // manage stock)
        interceptor.respondWith("wc-fetch-product-response-manage-stock-parent.json")
        productRestClient.fetchSingleProduct(siteModel, remoteProductId)
        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
        val payloadParent = lastAction!!.payload as RemoteProductPayload
        assertTrue(payloadParent.product.manageStock)
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
            assertNotNull(products)
            assertEquals(products.size, 3)
        }

        // delete all products then insert these into the store
        ProductSqlUtils.deleteProductsForSite(siteModel)
        assertEquals(ProductSqlUtils.insertOrUpdateProducts(payload.products), 3)

        // now verify the db stored the products correctly
        val productsFromDb = ProductSqlUtils.getProductsForSite(siteModel)
        assertNotNull(productsFromDb)
        assertEquals(productsFromDb.size, 3)
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
        productRestClient.searchProducts(siteModel, searchQuery)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.SEARCHED_PRODUCTS, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteSearchProductsPayload
        assertNull(payload.error)
        assertEquals(payload.searchQuery, searchQuery)
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
    fun testFetchProductVariationsSuccess() {
        interceptor.respondWith("wc-fetch-product-variations-response-success.json")
        productRestClient.fetchProductVariations(siteModel, remoteProductId)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_PRODUCT_VARIATIONS, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductVariationsPayload
        assertNull(payload.error)
        assertEquals(payload.remoteProductId, remoteProductId)
        assertEquals(payload.variations.size, 3)
        assertEquals(payload.variations[0].imageUrl, "")
        assertNotNull(payload.variations[1].imageUrl)

        // save the variation to the db
        assertEquals(ProductSqlUtils.insertOrUpdateProductVariations(payload.variations), 3)

        // now delete all variations for this product and save again
        ProductSqlUtils.deleteVariationsForProduct(siteModel, remoteProductId)
        assertEquals(ProductSqlUtils.insertOrUpdateProductVariations(payload.variations), 3)

        // now verify the db stored the variation correctly
        val dbVariations = ProductSqlUtils.getVariationsForProduct(siteModel, remoteProductId)
        assertEquals(dbVariations.size, 3)
        with(dbVariations.first()) {
            assertEquals(this.remoteProductId, remoteProductId)
            assertEquals(this.localSiteId, siteModel.id)

            // verify that the variant with the first menu order is fetched first
            assertEquals(this.menuOrder, 1)
        }
    }

    @Test
    fun testFetchProductVariationsError() {
        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        productRestClient.fetchProductVariations(siteModel, remoteProductId)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_PRODUCT_VARIATIONS, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductVariationsPayload
        assertNotNull(payload.error)
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
    fun testFetchProductReviewsSuccess() {
        interceptor.respondWith("wc-fetch-product-reviews-response-success.json")
        productRestClient.fetchProductReviews(siteModel, 0)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_PRODUCT_REVIEWS, lastAction!!.type)
        val payload = lastAction!!.payload as FetchProductReviewsResponsePayload
        assertFalse(payload.isError)
        assertEquals(siteModel.id, payload.site.id)
        assertEquals(25, payload.reviews.size)
        assertNull(payload.filterProductIds)
        assertNull(payload.filterByStatus)
        assertFalse(payload.loadedMore)
        assertTrue(payload.canLoadMore)

        // Save product reviews to the database
        assertEquals(25, ProductSqlUtils.insertOrUpdateProductReviews(payload.reviews))
        assertEquals(
                5,
                ProductSqlUtils.getProductReviewsForProductAndSiteId(siteModel.id, 22).size)
    }

    @Test
    fun testFetchProductReviewsFailed() {
        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        productRestClient.fetchProductReviews(siteModel, 0)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_PRODUCT_REVIEWS, lastAction!!.type)
        val payload = lastAction!!.payload as FetchProductReviewsResponsePayload
        assertTrue(payload.isError)
        assertEquals(ProductErrorType.GENERIC_ERROR, payload.error.type)
    }

    @Test
    fun testFetchProductReviewByReviewIdSuccess() {
        interceptor.respondWith("wc-fetch-product-review-response-success.json")
        productRestClient.fetchProductReviewById(siteModel, 5499)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_SINGLE_PRODUCT_REVIEW, lastAction!!.type)

        // Verify payload and product review properties
        val payload = lastAction!!.payload as RemoteProductReviewPayload
        assertFalse(payload.isError)
        assertEquals(siteModel.id, payload.site.id)
        payload.productReview?.let {
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
    }

    @Test
    fun testFetchProductReviewByReviewIdFailed() {
        interceptor.respondWithError("wc-product-review-response-failure-invalid-id.json")
        productRestClient.fetchProductReviewById(siteModel, 5499)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_SINGLE_PRODUCT_REVIEW, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductReviewPayload
        assertTrue(payload.isError)
        assertEquals(ProductErrorType.INVALID_REVIEW_ID, payload.error.type)
    }

    @Test
    fun testUpdateProductReviewStatusSuccess() {
        interceptor.respondWith("wc-update-product-review-response-success.json")
        productRestClient.updateProductReviewStatus(siteModel, 0, "spam")

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.UPDATED_PRODUCT_REVIEW_STATUS, lastAction!!.type)

        // Verify payload and product review properties
        val payload = lastAction!!.payload as RemoteProductReviewPayload
        assertFalse(payload.isError)
        assertEquals(siteModel.id, payload.site.id)
        payload.productReview?.let {
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

    @Test
    fun testUpdateProductReviewStatusFailed() {
        interceptor.respondWithError("wc-response-failure-invalid-param.json")
        productRestClient.updateProductReviewStatus(siteModel, 0, "spam")

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.UPDATED_PRODUCT_REVIEW_STATUS, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductReviewPayload
        assertTrue(payload.isError)
        assertEquals(ProductErrorType.INVALID_PARAM, payload.error.type)
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

    private fun generateTestProduct(): WCProductModel {
        return WCProductModel(1).also {
            it.localSiteId = siteModel.id
            it.remoteProductId = remoteProductId
            it.name = "Product name"
            it.sku = "34343-343776"
        }
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
            assertEquals(remoteProductId, product.remoteProductId)
            assertEquals(product.getImages().size, 2)
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
    fun testUpdateProductSuccess() {
        interceptor.respondWith("wc-fetch-product-response-success.json")

        val testProduct = generateTestProduct()
        val updatedProduct = testProduct.copy().apply {
            name = testProduct.name
            sku = testProduct.sku
            description = "Testing product description update"
        }
        productRestClient.updateProduct(siteModel, testProduct, updatedProduct)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.UPDATED_PRODUCT, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteUpdateProductPayload
        with(payload) {
            assertNull(error)
            assertEquals(remoteProductId, product.remoteProductId)
            assertEquals(updatedProduct.description, product.description)
            assertEquals(updatedProduct.name, product.name)
            assertEquals(updatedProduct.sku, product.sku)
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
        assertEquals(productBefore!!.getImages().size, 2)

        // remove one of the images
        val didDelete = ProductSqlUtils.deleteProductImage(siteModel, remoteProductId, 1)
        assertTrue(didDelete)

        // now make sure only one image is attached to the product
        val productAfter = ProductSqlUtils.getProductByRemoteId(siteModel, remoteProductId)
        assertNotNull(productAfter)
        assertEquals(productAfter!!.getImages().size, 1)
    }

    @Suppress("unused")
    @Subscribe
    fun onAction(action: Action<*>) {
        lastAction = action
        countDownLatch.countDown()
    }
}
