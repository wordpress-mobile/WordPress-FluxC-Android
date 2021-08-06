package org.wordpress.android.fluxc.release

import com.wellsql.generated.SiteModelTable
import org.wordpress.android.fluxc.example.BuildConfig
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.release.ReleaseStack_WCBase.SiteAwareAuthenticatePayload.TestSite.Specified
import org.wordpress.android.fluxc.release.ReleaseStack_WCBase.SiteAwareAuthenticatePayload.TestSite.FirstAvailable
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload

open class ReleaseStack_WCBase : ReleaseStack_WPComBase() {
    private val siteSqlUtils = SiteSqlUtils()

    protected open val siteAwareAuthenticatePayload = SiteAwareAuthenticatePayload(
            authenticatePayload = AuthenticatePayload(
                    BuildConfig.TEST_WPCOM_USERNAME_WOO_JETPACK,
                    BuildConfig.TEST_WPCOM_PASSWORD_WOO_JETPACK
            ),
            site = FirstAvailable
    )

    override fun getSiteFromDb(): SiteModel = when (val siteToTest = siteAwareAuthenticatePayload.site) {
        is Specified -> {
            try {
                mSiteStore.sites.first { databaseSite -> databaseSite.siteId == siteToTest.siteId }
            } catch (exception: NoSuchElementException) {
                throw AssertionError("This test account doesn't have site with id ${siteToTest.siteId}")
            }
        }
        is FirstAvailable -> {
            try {
                siteSqlUtils.getSitesWith(SiteModelTable.HAS_WOO_COMMERCE, true).asModel.first()
            } catch (exception: NoSuchElementException) {
                throw AssertionError("This test account doesn't seem to have any WooCommerce sites!")
            }
        }
    }

    override fun buildAuthenticatePayload() = siteAwareAuthenticatePayload.authenticatePayload

    data class SiteAwareAuthenticatePayload(
        val authenticatePayload: AuthenticatePayload,
        val site: TestSite
    ) {
        sealed class TestSite {
            object FirstAvailable : TestSite()
            data class Specified(val siteId: Long) : TestSite()
        }
    }
}
