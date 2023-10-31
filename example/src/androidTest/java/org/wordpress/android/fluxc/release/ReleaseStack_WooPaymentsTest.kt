package org.wordpress.android.fluxc.release

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.wordpress.android.fluxc.example.test.BuildConfig
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload
import org.wordpress.android.fluxc.store.WCWooPaymentsStore
import javax.inject.Inject

class ReleaseStack_WooPaymentsTest : ReleaseStack_WCBase() {
    @Inject internal lateinit var store: WCWooPaymentsStore

    override val testSite: TestSite = TestSite.Specified(siteId = BuildConfig.TEST_WPCOM_SITE_ID_WOO_JP_WCPAY.toLong())

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
    fun givenSiteHasWCPayWhenLoadAccountThenTestAccountReturned() = runBlocking {
        val result = store.fetchDepositsOverview(sSite)

        assertEquals("usd", result.result?.account?.defaultCurrency)
        assertEquals(false, result.result?.account?.depositsBlocked)
        assertEquals(true, result.result?.account?.depositsEnabled)
        assertEquals(2, result.result?.account?.depositsSchedule?.delayDays)
        assertEquals("daily", result.result?.account?.depositsSchedule?.interval)
        assertEquals(0, result.result?.balance?.available?.get(0)?.amount)
        assertEquals("usd", result.result?.balance?.available?.get(0)?.currency)
        assertEquals(0, result.result?.balance?.available?.get(0)?.sourceTypes?.card)
        assertEquals(0, result.result?.balance?.instant?.size)
        assertEquals(0, result.result?.balance?.pending?.get(0)?.amount)
        assertEquals("usd", result.result?.balance?.pending?.get(0)?.currency)
        assertEquals(0, result.result?.balance?.pending?.get(0)?.depositsCount)
        assertEquals(0, result.result?.balance?.pending?.get(0)?.sourceTypes?.card)
        assertEquals(0, result.result?.deposit?.lastManualDeposits?.size)
        assertEquals(1, result.result?.deposit?.lastPaid?.size)
        assertEquals(4373, result.result?.deposit?.lastPaid?.get(0)?.amount)
        assertEquals(true, result.result?.deposit?.lastPaid?.get(0)?.automatic)
        assertEquals("STRIPE TEST BANK •••• 6789 (USD)", result.result?.deposit?.lastPaid?.get(0)?.bankAccount)
        assertEquals(1644192000, result.result?.deposit?.lastPaid?.get(0)?.created)
        assertEquals("usd", result.result?.deposit?.lastPaid?.get(0)?.currency)
        assertEquals(1644192000000, result.result?.deposit?.lastPaid?.get(0)?.date)
        assertEquals(0, result.result?.deposit?.lastPaid?.get(0)?.fee)
        assertEquals(0, result.result?.deposit?.lastPaid?.get(0)?.feePercentage)
        assertEquals("po_1KQLho2HswaZkMX3M9Qhzf4W", result.result?.deposit?.lastPaid?.get(0)?.accountId)
        assertEquals("paid", result.result?.deposit?.lastPaid?.get(0)?.status)
        assertEquals("deposit", result.result?.deposit?.lastPaid?.get(0)?.type)
        assertEquals(0, result.result?.deposit?.nextScheduled?.size)
    }
}
