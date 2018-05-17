package org.wordpress.android.fluxc.release

import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class ReleaseStack_WCBaseStoreTest : ReleaseStack_WCBase() {
    internal enum class TestEvent {
        NONE
    }

    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

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
    fun testGetSites() {
        assertTrue(wooCommerceStore.getWooCommerceSites().isNotEmpty())
    }
}
