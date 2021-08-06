package org.wordpress.android.fluxc.release

import com.wellsql.generated.SiteModelTable
import org.wordpress.android.fluxc.example.BuildConfig
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.release.ReleaseStack_WCBase.TestSite.FirstAvailable
import org.wordpress.android.fluxc.release.ReleaseStack_WCBase.TestSite.Specified
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload

open class ReleaseStack_WCBase : ReleaseStack_WPComBase() {
    private val siteSqlUtils = SiteSqlUtils()
    private val defaultAuthenticatePayload = AuthenticatePayload(
            BuildConfig.TEST_WPCOM_USERNAME_WOO_JETPACK,
            BuildConfig.TEST_WPCOM_PASSWORD_WOO_JETPACK
    )

    protected open val testSite: TestSite = FirstAvailable

    override fun getSiteFromDb(): SiteModel = when (val site = testSite) {
        is Specified -> {
            try {
                mSiteStore.sites.first { databaseSite -> databaseSite.siteId == site.siteId }
            } catch (exception: NoSuchElementException) {
                throw AssertionError("This test account doesn't seem to have site with id ${site.siteId}")
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

    override fun buildAuthenticatePayload() = defaultAuthenticatePayload

    sealed class TestSite {
        object FirstAvailable : TestSite()
        data class Specified(val siteId: Long) : TestSite()
    }
}
