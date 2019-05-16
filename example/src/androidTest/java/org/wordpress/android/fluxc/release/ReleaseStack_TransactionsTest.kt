package org.wordpress.android.fluxc.release

import android.text.TextUtils
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertTrue
import junit.framework.Assert.fail
import org.greenrobot.eventbus.Subscribe
import org.junit.Assert
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.TransactionAction
import org.wordpress.android.fluxc.generated.TransactionActionBuilder
import org.wordpress.android.fluxc.model.DomainContactModel
import org.wordpress.android.fluxc.release.ReleaseStack_TransactionsTest.TestEvents.ERROR_REDEEMING_SHOPPING_CART_INSUFFICIENT_FUNDS
import org.wordpress.android.fluxc.release.ReleaseStack_TransactionsTest.TestEvents.ERROR_REDEEMING_SHOPPING_CART_WRONG_COUNTRY_CODE
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.TransactionsStore
import org.wordpress.android.fluxc.store.TransactionsStore.CreateShoppingCartPayload
import org.wordpress.android.fluxc.store.TransactionsStore.OnShoppingCartCreated
import org.wordpress.android.fluxc.store.TransactionsStore.OnShoppingCartRedeemed
import org.wordpress.android.fluxc.store.TransactionsStore.OnSupportedCountriesFetched
import org.wordpress.android.fluxc.store.TransactionsStore.RedeemShoppingCartPayload
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.COUNTRY_CODE
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.INSUFFICIENT_FUNDS
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class ReleaseStack_TransactionsTest : ReleaseStack_WPComBase() {
    @Suppress("unused")
    @Inject lateinit var transactionsStore: TransactionsStore // needs to be injected for test to work properly
    @Inject lateinit var siteStore: SiteStore
    private var nextEvent: TestEvents? = null

    companion object {
        private const val TEST_DOMAIN_NAME = "superraredomainname156726.blog"
        private const val TEST_DOMAIN_PRODUCT_ID = "76"
        private val TEST_DOMAIN_CONTACT_MODEL = DomainContactModel(
                "Wapu",
                "Wordpress",
                "WordPress",
                "7337 Publishing Row",
                "Apt 404",
                "90210",
                "Best City",
                "CA",
                "US",
                "wapu@wordpress.org",
                "+1.3120000000",
                null
        )
    }

    internal enum class TestEvents {
        NONE,
        COUNTRIES_FETCHED,
        SHOPPING_CART_CREATED,
        ERROR_REDEEMING_SHOPPING_CART_INSUFFICIENT_FUNDS,
        ERROR_REDEEMING_SHOPPING_CART_WRONG_COUNTRY_CODE
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
        mDispatcher.dispatch(
                TransactionActionBuilder.generateNoPayloadAction(
                        TransactionAction.FETCH_SUPPORTED_COUNTRIES
                )
        )
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
    }

    @Test
    fun testCreateShoppingCard() {
        nextEvent = TestEvents.SHOPPING_CART_CREATED
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
                TransactionActionBuilder.newCreateShoppingCartAction(
                        CreateShoppingCartPayload(sSite, TEST_DOMAIN_PRODUCT_ID, TEST_DOMAIN_NAME, true)
                )
        )
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
    }

    @Test
    fun testRedeemingCardWithoutDomainCredit() {
        nextEvent = ERROR_REDEEMING_SHOPPING_CART_INSUFFICIENT_FUNDS
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
                TransactionActionBuilder.newCreateShoppingCartAction(
                        CreateShoppingCartPayload(sSite, TEST_DOMAIN_PRODUCT_ID, TEST_DOMAIN_NAME, true)
                )
        )
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
    }

    @Test
    fun testRedeemingCardWithWrongCountryCode() {
        nextEvent = ERROR_REDEEMING_SHOPPING_CART_WRONG_COUNTRY_CODE
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
                TransactionActionBuilder.newCreateShoppingCartAction(
                        CreateShoppingCartPayload(sSite, TEST_DOMAIN_PRODUCT_ID, TEST_DOMAIN_NAME, true)
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
        Assert.assertFalse(event.countries!!.any { TextUtils.isEmpty(it.name) || TextUtils.isEmpty(it.code) })
        mCountDownLatch.countDown()
    }

    @Subscribe
    @Suppress("unused")
    fun onShoppingCartCreated(event: OnShoppingCartCreated) {
        if (event.isError) {
            throw AssertionError("Unexpected error occurred with type: " + event.error.type)
        }

        assertNotNull(event.cartDetails)
        assertEquals(event.cartDetails!!.blog_id.toLong(), sSite.siteId)
        assertEquals(event.cartDetails!!.cart_key, sSite.siteId.toString())

        assertNotNull(event.cartDetails!!.products)
        assertTrue(event.cartDetails!!.products!!.isNotEmpty())
        assertEquals(event.cartDetails!!.products!!.size, 2)

        assertEquals(event.cartDetails!!.products!![0].product_id, TEST_DOMAIN_PRODUCT_ID)
        assertEquals(event.cartDetails!!.products!![0].meta, TEST_DOMAIN_NAME)

        if (nextEvent == ERROR_REDEEMING_SHOPPING_CART_INSUFFICIENT_FUNDS) {
            mDispatcher.dispatch(
                    TransactionActionBuilder.newRedeemCartWithCreditsAction(
                            RedeemShoppingCartPayload(
                                    event.cartDetails!!, TEST_DOMAIN_CONTACT_MODEL
                            )
                    )
            )

            return
        } else if (nextEvent == ERROR_REDEEMING_SHOPPING_CART_WRONG_COUNTRY_CODE) {
            val contactModelWithWrongCountryCode = TEST_DOMAIN_CONTACT_MODEL.copy(countryCode = "USB")

            mDispatcher.dispatch(
                    TransactionActionBuilder.newRedeemCartWithCreditsAction(
                            RedeemShoppingCartPayload(
                                    event.cartDetails!!, contactModelWithWrongCountryCode
                            )
                    )
            )
            return
        }

        mCountDownLatch.countDown()
    }

    @Subscribe
    @Suppress("unused")
    fun onShoppingCartRedeemed(event: OnShoppingCartRedeemed) {
        assertNotNull(event)
        assertFalse(event.success)
        assertNotNull(event.error)

        when (nextEvent) {
            ERROR_REDEEMING_SHOPPING_CART_WRONG_COUNTRY_CODE -> assertEquals(COUNTRY_CODE, event.error.type)
            ERROR_REDEEMING_SHOPPING_CART_INSUFFICIENT_FUNDS -> assertEquals(INSUFFICIENT_FUNDS, event.error.type)
            else -> fail("Unexpected test event: $nextEvent")
        }

        mCountDownLatch.countDown()
    }
}
