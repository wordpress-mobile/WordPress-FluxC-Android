package org.wordpress.android.fluxc.mocked

import org.greenrobot.eventbus.Subscribe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.WCProductAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductPayload
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

    private val productId = 1537L
    private val variationId = 193L

    private val siteModel = SiteModel().apply {
        id = 5
        siteId = 567
    }

    private val productModel = WCProductModel().apply {
        id = 1
        localSiteId = 5
        remoteProductId = productId
        remoteVariationId = variationId
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mMockedNetworkAppComponent.inject(this)
        dispatcher.register(this)
        lastAction = null
    }

    @Test
    fun testFetchSingleProductSuccess() {
        interceptor.respondWith("wc-fetch-product-response-success.json")
        productRestClient.fetchSingleProduct(siteModel, productId)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_SINGLE_PRODUCT, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductPayload
        with(payload) {
            assertNull(error)
            assertEquals(productId, product.remoteProductId)
            assertEquals(product.getCategories().size, 2)
            assertEquals(product.getTags().size, 2)
            assertEquals(product.getImages().size, 2)
            assertNotNull(product.getFirstImageUrl())
            assertEquals(product.getAttributes().size, 2)
            assertEquals(product.getAttributes()[0].options.size, 3)
            assertEquals(product.getAttributes()[0].getCommaSeparatedOptions(), "Small, Medium, Large")
        }

        // save the product to the db
        assertEquals(ProductSqlUtils.insertOrUpdateProduct(payload.product), 1)

        // now verify the db stored the product correctly
        val productFromDb = ProductSqlUtils.getProductByRemoteId(siteModel, productId)
        assertNotNull(productFromDb)
        productFromDb?.let { product ->
            assertEquals(product.remoteProductId, productId)
            assertEquals(product.getCategories().size, 2)
            assertEquals(product.getTags().size, 2)
            assertEquals(product.getImages().size, 2)
            assertNotNull(product.getFirstImageUrl())
            assertEquals(product.getAttributes().size, 2)
            assertEquals(product.getAttributes()[0].options.size, 3)
            assertEquals(product.getAttributes()[0].getCommaSeparatedOptions(), "Small, Medium, Large")
        }
    }

    @Test
    fun testFetchSingleProductError() {
        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        productRestClient.fetchSingleProduct(siteModel, productId)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_SINGLE_PRODUCT, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductPayload
        assertNotNull(payload.error)
    }

    @Test
    fun testFetchSingleProductVariationSuccess() {
        interceptor.respondWith("wc-fetch-product-single-variation-response-success.json")
        productRestClient.fetchProductVariation(siteModel, productModel, variationId)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_SINGLE_PRODUCT, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductPayload
        assertNull(payload.error)
        assertNotNull(payload.product)
        assertEquals(payload.product.remoteProductId, productId)
        assertEquals(payload.variationId, variationId)

        // save the product variation to the db
        assertEquals(ProductSqlUtils.insertOrUpdateProduct(payload.product), 1)

        // now delete all products and save again
        ProductSqlUtils.deleteProductsForSite(siteModel)
        assertEquals(ProductSqlUtils.insertOrUpdateProduct(payload.product), 1)

        // verify the db stored only this product
        val count = ProductSqlUtils.getProductCountForSite(siteModel, excludeVariations = false)
        assertEquals(count, 1)

        // verify single variation stored correctly
        val variation = ProductSqlUtils.getProductByRemoteId(siteModel, productId, variationId)
        assertNotNull(variation)
        variation?.let {
            assertEquals(it.remoteProductId, productId)
            assertEquals(it.remoteVariationId, variationId)
            assertEquals(it.localSiteId, siteModel.id)
        }
    }

    @Test
    fun testFetchSingleProductVariationError() {
        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        productRestClient.fetchSingleProduct(siteModel, productId, variationId)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCProductAction.FETCHED_SINGLE_PRODUCT, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteProductPayload
        assertNotNull(payload.error)
    }

    @Suppress("unused")
    @Subscribe
    fun onAction(action: Action<*>) {
        lastAction = action
        countDownLatch.countDown()
    }
}
