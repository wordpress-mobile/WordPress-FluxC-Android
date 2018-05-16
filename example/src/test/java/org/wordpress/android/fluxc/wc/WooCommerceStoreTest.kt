package org.wordpress.android.fluxc.wc

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WooCommerceStore
import kotlin.test.assertEquals

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WooCommerceStoreTest {
    private val wooCommerceStore = WooCommerceStore(Dispatcher())

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext

        val config = SingleStoreWellSqlConfigForTests(appContext, SiteModel::class.java,
                WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun testGetWooCommerceSites() {
        val nonWooSite = SiteModel().apply { siteId = 42 }
        WellSql.insert(nonWooSite).execute()

        assertEquals(0, wooCommerceStore.getWooCommerceSites().size)

        val wooJetpackSite = SiteModel().apply {
            siteId = 43
            hasWooCommerce = true
            setIsWPCom(false)
        }
        WellSql.insert(wooJetpackSite).execute()

        assertEquals(1, wooCommerceStore.getWooCommerceSites().size)

        val wooAtomicSite = SiteModel().apply {
            siteId = 44
            hasWooCommerce = true
            setIsWPCom(true)
        }
        WellSql.insert(wooAtomicSite).execute()

        assertEquals(2, wooCommerceStore.getWooCommerceSites().size)
    }
}
