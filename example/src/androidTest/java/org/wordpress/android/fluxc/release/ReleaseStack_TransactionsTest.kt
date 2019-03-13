package org.wordpress.android.fluxc.release

import org.greenrobot.eventbus.Subscribe
import org.junit.Assert
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.TransactionAction
import org.wordpress.android.fluxc.generated.TransactionActionBuilder
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.TransactionsStore
import org.wordpress.android.fluxc.store.TransactionsStore.CreateShoppingCartPayload
import org.wordpress.android.fluxc.store.TransactionsStore.OnShoppingCartCreated
import org.wordpress.android.fluxc.store.TransactionsStore.OnSupportedCountriesFetched
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class ReleaseStack_TransactionsTest : ReleaseStack_WPComBase() {
    @Suppress("unused")
    @Inject lateinit var transactionsStore: TransactionsStore // needs to be injected for test to work properly
    @Inject lateinit var siteStore: SiteStore
    private var nextEvent: TestEvents? = null

    internal enum class TestEvents {
        NONE,
        COUNTRIES_FETCHED,
        SHOPPING_CART_CREATED
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        // Register
        init()
        // Reset expected test event
        nextEvent = TestEvents.NONE
    }

    @Test
    fun testFetchCountries() {
        nextEvent = TestEvents.COUNTRIES_FETCHED
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(TransactionActionBuilder.generateNoPayloadAction(TransactionAction.FETCH_SUPPORTED_COUNTRIES))
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
    }

    @Test
    fun testCreateShoppingCard() {
        nextEvent = TestEvents.SHOPPING_CART_CREATED
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
                TransactionActionBuilder.newCreateShoppingCartAction(
                        CreateShoppingCartPayload(sSite, "76", "cooldomain.blog", true)
                )
        )
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
    }

    @Subscribe
    @Suppress("unused")
    fun onSupportedCountriesFetched(event: OnSupportedCountriesFetched) {
        if (event.isError) {
            throw AssertionError("Unexpected error occurred with type: " + event.error.type)
        }

        Assert.assertNotNull(event.countries)
        Assert.assertTrue(event.countries!!.isNotEmpty())
        mCountDownLatch.countDown()
    }

    @Subscribe
    @Suppress("unused")
    fun onShoppingCartCreated(event: OnShoppingCartCreated) {
        if (event.isError) {
            throw AssertionError("Unexpected error occurred with type: " + event.error.type)
        }

        Assert.assertNotNull(event.cartDetails)
        Assert.assertEquals(event.cartDetails!!.blog_id.toLong(), sSite.siteId)
        Assert.assertEquals(event.cartDetails!!.cart_key, sSite.siteId.toString())

        Assert.assertNotNull(event.cartDetails!!.products)
        Assert.assertTrue(event.cartDetails!!.products!!.isNotEmpty())
        Assert.assertEquals(event.cartDetails!!.products!!.size, 3)


        Assert.assertEquals(event.cartDetails!!.products!![0].product_id, 76)
        Assert.assertEquals(event.cartDetails!!.products!![0].meta, "cooldomain.blog")
        Assert.assertEquals(event.cartDetails!!.products!![1].product_id, 16)
        Assert.assertEquals(event.cartDetails!!.products!![1].meta, "cooldomain.blog")
        Assert.assertEquals(event.cartDetails!!.products!![2].product_id, 1003)
        Assert.assertEquals(event.cartDetails!!.products!![2].meta, "")

        mCountDownLatch.countDown()
    }
}
