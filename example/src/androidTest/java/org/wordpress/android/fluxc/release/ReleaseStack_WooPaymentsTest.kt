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
    fun whenFetchingDepositOverviewThenDataReturned() = runBlocking {
        val result = store.fetchDepositsOverview(sSite)

        assertEquals("usd", result.result?.account?.defaultCurrency)
        assertEquals(false, result.result?.account?.depositsBlocked)
        assertEquals(true, result.result?.account?.depositsEnabled)
        assertEquals(2, result.result?.account?.depositsSchedule?.delayDays)
        assertEquals("daily", result.result?.account?.depositsSchedule?.interval)
        assertEquals(0, result.result?.balance?.available?.get(0)?.amount)
        assertEquals(null, result.result?.balance?.available?.get(0)?.depositsCount)
        assertEquals("usd", result.result?.balance?.available?.get(0)?.currency)
        assertEquals(0, result.result?.balance?.available?.get(0)?.sourceTypes?.card)
        assertEquals(0, result.result?.balance?.instant?.size)
        assertEquals(0, result.result?.balance?.pending?.get(0)?.amount)
        assertEquals("usd", result.result?.balance?.pending?.get(0)?.currency)
        assertEquals(null, result.result?.balance?.pending?.get(0)?.fee)
        assertEquals(null, result.result?.balance?.pending?.get(0)?.feePercentage)
        assertEquals(null, result.result?.balance?.pending?.get(0)?.net)
        assertEquals(0, result.result?.balance?.pending?.get(0)?.depositsCount)
        assertEquals(0, result.result?.balance?.pending?.get(0)?.sourceTypes?.card)
        assertEquals(0, result.result?.deposit?.lastManualDeposits?.size)
        assertEquals(1, result.result?.deposit?.lastPaid?.size)
        assertEquals(4373, result.result?.deposit?.lastPaid?.get(0)?.amount)
        assertEquals(true, result.result?.deposit?.lastPaid?.get(0)?.automatic)
        assertEquals("STRIPE TEST BANK •••• 6789 (USD)", result.result?.deposit?.lastPaid?.get(0)?.bankAccount)
        assertEquals(1644192000L, result.result?.deposit?.lastPaid?.get(0)?.created)
        assertEquals("usd", result.result?.deposit?.lastPaid?.get(0)?.currency)
        assertEquals(1644192000000L, result.result?.deposit?.lastPaid?.get(0)?.date)
        assertEquals(0L, result.result?.deposit?.lastPaid?.get(0)?.fee)
        assertEquals(0.0, result.result?.deposit?.lastPaid?.get(0)?.feePercentage)
        assertEquals("po_1KQLho2HswaZkMX3M9Qhzf4W", result.result?.deposit?.lastPaid?.get(0)?.depositId)
        assertEquals("paid", result.result?.deposit?.lastPaid?.get(0)?.status)
        assertEquals("deposit", result.result?.deposit?.lastPaid?.get(0)?.type)
        assertEquals(0, result.result?.deposit?.nextScheduled?.size)
    }

    @Test
    fun whenSavingDepositOverviewAndGetThenDataReturned() = runBlocking {
        // GIVEN
        val fetchResult = store.fetchDepositsOverview(sSite)

        // WHEN
        store.insertDepositsOverview(sSite, fetchResult.result!!)

        // THEN
        val getResult = store.getDepositsOverviewAll(sSite)

        assertEquals("usd", getResult?.account?.defaultCurrency)
        assertEquals(false, getResult?.account?.depositsBlocked)
        assertEquals(true, getResult?.account?.depositsEnabled)
        assertEquals(2, getResult?.account?.depositsSchedule?.delayDays)
        assertEquals("daily", getResult?.account?.depositsSchedule?.interval)
        assertEquals(0, getResult?.balance?.available?.get(0)?.amount)
        assertEquals(null, getResult?.balance?.available?.get(0)?.depositsCount)
        assertEquals("usd", getResult?.balance?.available?.get(0)?.currency)
        assertEquals(0, getResult?.balance?.available?.get(0)?.sourceTypes?.card)
        assertEquals(0, getResult?.balance?.instant?.size)
        assertEquals(0, getResult?.balance?.pending?.get(0)?.amount)
        assertEquals("usd", getResult?.balance?.pending?.get(0)?.currency)
        assertEquals(null, getResult?.balance?.pending?.get(0)?.fee)
        assertEquals(null, getResult?.balance?.pending?.get(0)?.feePercentage)
        assertEquals(null, getResult?.balance?.pending?.get(0)?.net)
        assertEquals(0, getResult?.balance?.pending?.get(0)?.depositsCount)
        assertEquals(0, getResult?.balance?.pending?.get(0)?.sourceTypes?.card)
        assertEquals(0, getResult?.deposit?.lastManualDeposits?.size)
        assertEquals(1, getResult?.deposit?.lastPaid?.size)
        assertEquals(4373, getResult?.deposit?.lastPaid?.get(0)?.amount)
        assertEquals(true, getResult?.deposit?.lastPaid?.get(0)?.automatic)
        assertEquals("STRIPE TEST BANK •••• 6789 (USD)", getResult?.deposit?.lastPaid?.get(0)?.bankAccount)
        assertEquals(1644192000L, getResult?.deposit?.lastPaid?.get(0)?.created)
        assertEquals("usd", getResult?.deposit?.lastPaid?.get(0)?.currency)
        assertEquals(1644192000000L, getResult?.deposit?.lastPaid?.get(0)?.date)
        assertEquals(0L, getResult?.deposit?.lastPaid?.get(0)?.fee)
        assertEquals(0.0, getResult?.deposit?.lastPaid?.get(0)?.feePercentage)
        assertEquals("po_1KQLho2HswaZkMX3M9Qhzf4W", getResult?.deposit?.lastPaid?.get(0)?.depositId)
        assertEquals("paid", getResult?.deposit?.lastPaid?.get(0)?.status)
        assertEquals("deposit", getResult?.deposit?.lastPaid?.get(0)?.type)
        assertEquals(0, getResult?.deposit?.nextScheduled?.size)

    }

    @Test
    fun givenSavedDepositWhenDepositDeleteThenNullStored() = runBlocking {
        // GIVEN
        val fetchResult = store.fetchDepositsOverview(sSite)
        store.insertDepositsOverview(sSite, fetchResult.result!!)

        // WHEN
        store.deleteDepositsOverview(sSite)

        // THEN
        val getResult = store.getDepositsOverviewAll(sSite)
        assertEquals(null, getResult)
    }
}
