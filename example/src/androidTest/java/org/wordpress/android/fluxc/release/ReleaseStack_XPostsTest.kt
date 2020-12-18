package org.wordpress.android.fluxc.release

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.example.BuildConfig
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload
import org.wordpress.android.fluxc.store.XPostsResult
import org.wordpress.android.fluxc.store.XPostsSource
import org.wordpress.android.fluxc.store.XPostsStore
import javax.inject.Inject

class ReleaseStack_XPostsTest : ReleaseStack_WPComBase() {
    @Inject lateinit var xPostsStore: XPostsStore

    override fun buildAuthenticatePayload(): AuthenticatePayload =
            AuthenticatePayload(BuildConfig.TEST_WPCOM_USERNAME_XPOSTS, BuildConfig.TEST_WPCOM_PASSWORD_XPOSTS)

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        init()
    }

    @Test
    fun makes_successful_call_returning_xpost() {
        val response = runBlocking {
            val site = SiteModel().apply { siteId = siteFromDb.siteId }
            xPostsStore.fetchXPosts(site)
        }

        assertTrue(response is XPostsResult.Result)
        assertEquals(XPostsSource.REST_API, (response as XPostsResult.Result).source)
        assertTrue(response.xPosts.isNotEmpty())
    }
}
