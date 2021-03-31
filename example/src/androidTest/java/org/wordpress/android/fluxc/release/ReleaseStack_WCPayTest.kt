package org.wordpress.android.fluxc.release

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.example.test.BuildConfig
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload
import org.wordpress.android.fluxc.store.WCPayStore
import javax.inject.Inject

class ReleaseStack_WCPayTest : ReleaseStack_WCBase() {
    @Inject internal lateinit var payStore: WCPayStore

    override fun buildAuthenticatePayload() = AuthenticatePayload(
            BuildConfig.TEST_WPCOM_USERNAME_WOO_JP_WCPAY,
            BuildConfig.TEST_WPCOM_PASSWORD_WOO_JP_WCPAY
    )

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        // Register
        init()
    }

    @Test
    fun givenSiteHasWCPayWhenFetchConnectionTokenInvokedThenTokenReturned() = runBlocking {
        val result = payStore.fetchConnectionToken(sSite)

        assertTrue(result.model?.token?.isNotEmpty() == true)
    }
}
