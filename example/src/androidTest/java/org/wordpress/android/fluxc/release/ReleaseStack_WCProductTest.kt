package org.wordpress.android.fluxc.release

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.WCProductAction
import org.wordpress.android.fluxc.example.BuildConfig
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductReviewsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductVariationsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductReviewPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductReviewStatusPayload
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class ReleaseStack_WCProductTest : ReleaseStack_WCBase() {
    internal enum class TestEvent {
        NONE,
        FETCHED_SINGLE_PRODUCT,
        FETCHED_PRODUCT_VARIATIONS,
        FETCHED_PRODUCT_REVIEWS,
        FETCHED_SINGLE_PRODUCT_REVIEW,
        UPDATED_PRODUCT_REVIEW_STATUS
    }

    @Inject internal lateinit var productStore: WCProductStore

    private var nextEvent: TestEvent = TestEvent.NONE
    private val productModel = WCProductModel(8).apply {
        remoteProductId = BuildConfig.TEST_WC_PRODUCT_ID.toLong()
        dateCreated = "2018-04-20T15:45:14Z"
    }
    private val productModelWithVariations = WCProductModel(8).apply {
        remoteProductId = BuildConfig.TEST_WC_PRODUCT_WITH_VARIATIONS_ID.toLong()
        dateCreated = "2018-04-20T15:45:14Z"
    }
    private val remoteProductWithReviewsId = BuildConfig.TEST_WC_PRODUCT_WITH_REVIEWS_ID.toLong()
    private val remoteProductReviewId = BuildConfig.TEST_WC_PRODUCT_REVIEW_ID.toLong()

    private var lastEvent: OnProductChanged? = null

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp(false)
        mReleaseStackAppComponent.inject(this)
        init()
        nextEvent = TestEvent.NONE
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchSingleProduct() {
        // remove all products for this site and verify there are none
        ProductSqlUtils.deleteProductsForSite(sSite)
        assertEquals(ProductSqlUtils.getProductCountForSite(sSite), 0)

        nextEvent = TestEvent.FETCHED_SINGLE_PRODUCT
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
                WCProductActionBuilder
                        .newFetchSingleProductAction(FetchSingleProductPayload(sSite, productModel.remoteProductId))
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify results
        val fetchedProduct = productStore.getProductByRemoteId(sSite, productModel.remoteProductId)
        assertNotNull(fetchedProduct)
        assertEquals(fetchedProduct!!.remoteProductId, productModel.remoteProductId)

        // Verify there's only one product for this site
        assertEquals(ProductSqlUtils.getProductCountForSite(sSite), 1)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchProductVariations() {
        // remove all variations for this product and verify there are none
        ProductSqlUtils.deleteVariationsForProduct(sSite, productModelWithVariations.remoteProductId)
        assertEquals(ProductSqlUtils.getVariationsForProduct(sSite, productModelWithVariations.remoteProductId).size, 0)

        nextEvent = TestEvent.FETCHED_PRODUCT_VARIATIONS
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
                WCProductActionBuilder
                        .newFetchProductVariationsAction(
                                FetchProductVariationsPayload(
                                        sSite,
                                        productModelWithVariations.remoteProductId
                                )
                        )
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify results
        val fetchedVariations = productStore.getVariationsForProduct(sSite, productModelWithVariations.remoteProductId)
        assertNotEquals(fetchedVariations.size, 0)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchProductReviews() {
        // Remove all product reviews from the database
        productStore.deleteAllProductReviews()
        assertEquals(0, ProductSqlUtils.getProductReviewsForSite(sSite).size)

        nextEvent = TestEvent.FETCHED_PRODUCT_REVIEWS
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
                WCProductActionBuilder.newFetchProductReviewsAction(FetchProductReviewsPayload(sSite, offset = 0)))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify results
        val fetchedReviews = productStore.getProductReviewsForSite(sSite)
        assertTrue(fetchedReviews.isNotEmpty())
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchSingleProductAndUpdateReview() {
        // Remove all product reviews from the database
        productStore.deleteAllProductReviews()
        assertEquals(0, ProductSqlUtils.getProductReviewsForSite(sSite).size)

        nextEvent = TestEvent.FETCHED_SINGLE_PRODUCT_REVIEW
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
                WCProductActionBuilder.newFetchSingleProductReviewAction(
                        FetchSingleProductReviewPayload(sSite, remoteProductReviewId)))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify results
        val review = productStore
                .getProductReviewByRemoteId(sSite.id, remoteProductReviewId)
        assertNotNull(review)

        // Update review status
        review?.let {
            val newStatus = when (it.status) {
                "hold", "approved", "unapproved", "trash", "unspam", "untrash" -> "spam"
                else -> "approved"
            }
            nextEvent = TestEvent.UPDATED_PRODUCT_REVIEW_STATUS
            mCountDownLatch = CountDownLatch(1)
            mDispatcher.dispatch(
                    WCProductActionBuilder.newUpdateProductReviewStatusAction(
                            UpdateProductReviewStatusPayload(sSite, review, newStatus)))
            assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

            // Verify results
            val savedReview = productStore
                    .getProductReviewByRemoteId(sSite.id, remoteProductReviewId)
            assertNotNull(savedReview)
            assertEquals(newStatus, savedReview!!.status)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductChanged(event: OnProductChanged) {
        event.error?.let {
            throw AssertionError("OnProductChanged has unexpected error: " + it.type)
        }

        lastEvent = event

        when (event.causeOfChange) {
            WCProductAction.FETCH_SINGLE_PRODUCT -> {
                assertEquals(TestEvent.FETCHED_SINGLE_PRODUCT, nextEvent)
                mCountDownLatch.countDown()
            }
            WCProductAction.FETCH_PRODUCT_VARIATIONS -> {
                assertEquals(TestEvent.FETCHED_PRODUCT_VARIATIONS, nextEvent)
                mCountDownLatch.countDown()
            }
            WCProductAction.FETCH_SINGLE_PRODUCT_REVIEW -> {
                assertEquals(TestEvent.FETCHED_SINGLE_PRODUCT_REVIEW, nextEvent)
                mCountDownLatch.countDown()
            }
            WCProductAction.FETCH_PRODUCT_REVIEWS -> {
                assertEquals(TestEvent.FETCHED_PRODUCT_REVIEWS, nextEvent)
                mCountDownLatch.countDown()
            }
            WCProductAction.UPDATE_PRODUCT_REVIEW_STATUS -> {
                assertEquals(TestEvent.UPDATED_PRODUCT_REVIEW_STATUS, nextEvent)
                mCountDownLatch.countDown()
            }
            else -> throw AssertionError("Unexpected cause of change: " + event.causeOfChange)
        }
    }
}
