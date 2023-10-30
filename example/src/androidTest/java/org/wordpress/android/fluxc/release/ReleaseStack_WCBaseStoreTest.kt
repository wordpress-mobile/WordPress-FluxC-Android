package org.wordpress.android.fluxc.release

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.wordpress.android.fluxc.persistence.dao.TaxBasedOnDao
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class ReleaseStack_WCBaseStoreTest : ReleaseStack_WCBase() {
    internal enum class TestEvent {
        NONE
    }

    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var taxBasedOnDao: TaxBasedOnDao

    private var nextEvent: TestEvent = TestEvent.NONE

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        // Register
        init()
        // Reset expected test event
        nextEvent = TestEvent.NONE
    }

    @Throws(InterruptedException::class)
    @Test
    @Ignore
    fun testGetSites() {
        assertTrue(wooCommerceStore.getWooCommerceSites().isNotEmpty())
    }

    @Test
    @Ignore
    fun testFetchTaxBasedOnSetting() = runBlocking {
        val site = siteFromDb
        val result = wooCommerceStore.fetchTaxBasedOnSettings(site)
        assertThat(result.isError).isFalse()
        assertThat(result.model).isNotNull
        Unit
    }

    @Test
    fun testGetTaxBasedOnSetting() = runBlocking {
        val site = siteFromDb
        val result = wooCommerceStore.fetchTaxBasedOnSettings(site)
        assertThat(result.isError).isFalse()
        assertThat(result.model).isNotNull
        val setting = result.model
        with(taxBasedOnDao.getTaxBasedOnSetting(site.localId())) {
            assertThat(this).isNotNull
            assertThat(this).isEqualTo(setting)
        }
        Unit
    }
}
