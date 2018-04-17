package org.wordpress.android.fluxc.release

import com.wellsql.generated.SiteModelTable
import org.wordpress.android.fluxc.example.BuildConfig
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload

open class ReleaseStack_WCBase : ReleaseStack_WPComBase() {
    private val authenticatePayload by lazy {
        AuthenticatePayload(BuildConfig.TEST_WPCOM_USERNAME_WOO_JETPACK,
                BuildConfig.TEST_WPCOM_PASSWORD_WOO_JETPACK)
    }

    override fun getSiteFromDb(): SiteModel {
        val wcSites = SiteSqlUtils.getSitesWith(SiteModelTable.HAS_WOO_COMMERCE, true).asModel
        if (wcSites.isEmpty()) {
            throw AssertionError("This test account doesn't seem to have any WooCommerce sites!")
        } else {
            return wcSites[0]
        }
    }

    override fun buildAuthenticatePayload() = authenticatePayload
}
