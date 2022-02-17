package org.wordpress.android.fluxc.release

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.wordpress.android.fluxc.example.test.BuildConfig
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore.InPersonPaymentsPluginType.STRIPE
import javax.inject.Inject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.util.Locale

class ReleaseStack_InPersonPaymentsStripeExtensionTest : ReleaseStack_WCBase() {
    @Inject internal lateinit var store: WCInPersonPaymentsStore

    override val testSite: TestSite = TestSite.Specified(
            siteId = BuildConfig.TEST_WPCOM_SITE_ID_WOO_JP_STRIPE_EXTENSION.toLong()
    )

    override fun buildAuthenticatePayload() = AuthenticatePayload(
            BuildConfig.TEST_WPCOM_USERNAME_WOO_JP,
            BuildConfig.TEST_WPCOM_PASSWORD_WOO_JP
    )

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        // Register
        init()
    }

    @Test
    fun givenSiteHasStripeExtensionWhenFetchConnectionTokenInvokedThenTokenReturned() = runBlocking {
        val result = store.fetchConnectionToken(STRIPE, sSite)

        assertTrue(result.model?.token?.isNotEmpty() == true)
    }

    @Test
    fun givenSiteHasStripeExtensionWhenLoadAccountThenTestAccountReturned() = runBlocking {
        val result = store.loadAccount(STRIPE, sSite)

        assertEquals("US", result.model?.country)
        assertEquals(false, result.model?.hasPendingRequirements)
        assertEquals(false, result.model?.hasOverdueRequirements)
        assertEquals(result.model?.statementDescriptor?.toLowerCase(Locale.ROOT), "custom descriptor")
        assertEquals("US", result.model?.country)
        assertEquals("usd", result.model?.storeCurrencies?.default)
        assertEquals(listOf("usd"), result.model?.storeCurrencies?.supportedCurrencies)
        assertEquals(WCPaymentAccountStatus.COMPLETE, result.model?.status)
    }

    @Test
    fun givenSiteHasStripeExtensionAndOrderWhenCreateCustomerByOrderIdCustomerIdReturned() = runBlocking {
        val result = store.createCustomerByOrderId(
            STRIPE,
                sSite,
                17L
        )

        assertEquals("cus_KpWfupr71lMX0W", result.model?.customerId)
    }

    @Test
    fun givenSiteHasStripeExtensionAndWrongOrderIdWhenCreateCustomerByOrderIdCustomerIdReturned() = runBlocking {
        val result = store.createCustomerByOrderId(
            STRIPE,
                sSite,
                1L
        )

        assertTrue(result.isError)
    }

    @Test
    fun givenSiteHasStripeExtensionAndStripeAddressThenLocationDataReturned() = runBlocking {
        val result = store.getStoreLocationForSite(STRIPE, sSite)

        assertFalse(result.isError)
        assertEquals("tml_EbIYQbo6EsyAee", result.locationId)
        assertEquals("Woo Jetpack Stripe Extension", result.displayName)
        assertEquals("San Francisco", result.address?.city)
        assertEquals("US", result.address?.country)
        assertEquals("1230 Lawton St", result.address?.line1)
        assertEquals("", result.address?.line2)
        assertEquals("94122", result.address?.postalCode)
        assertEquals("CA", result.address?.state)
    }
}
