package org.wordpress.android.fluxc.release

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.wordpress.android.fluxc.example.BuildConfig
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.FetchWPAPISitePayload
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

/**
 * Tests with real credentials on real servers using the full release stack (no mock).
 * Skips self hosted site discovery, directly using the ENDPOINT URLs from tests.properties.
 */
class ReleaseStack_SiteTestWPAPI : ReleaseStack_Base() {
    @Inject lateinit var siteStore: SiteStore

    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
    }

    @Test()
    fun testFetchingSiteUsingWPRESTAPI() = runBlocking {
        val payload = FetchWPAPISitePayload(
            url = BuildConfig.TEST_WPORG_URL_SH_SIMPLE,
            username = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE,
            password = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE
        )

        siteStore.fetchWPAPISite(payload)

        val site = siteStore.getSitesByNameOrUrlMatching(BuildConfig.TEST_WPORG_URL_SH_SIMPLE)
            .first()

        Assert.assertTrue(site.name.isNotEmpty())
        Assert.assertEquals(
            UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_SIMPLE),
            UrlUtils.removeScheme(site.url)
        )
        Assert.assertTrue(site.email.isNotEmpty())
        Assert.assertEquals(SiteModel.ORIGIN_WPAPI, site.origin)
        Assert.assertEquals(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE, site.username)
        Assert.assertEquals(BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE, site.password)
    }
}
