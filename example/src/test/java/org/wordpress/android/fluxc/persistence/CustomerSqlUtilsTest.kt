package org.wordpress.android.fluxc.persistence

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class CustomerSqlUtilsTest {
    val site = SiteModel().apply {
        email = "test@example.org"
        name = "Test Site"
        siteId = 24
    }

    @Before
    fun setUp() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(WCCustomerModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun `get customer by remote id returns null when there is no customer stored`() {
        // given
        val remoteCustomerId = 1L

        // when & then
        assertNull(CustomerSqlUtils.getCustomerByRemoteId(site, remoteCustomerId))
    }

    @Test
    fun `get customer by remote id returns customer when there its stored`() {
        // given
        val remoteCustomerId = 1L
        val username = "userName"
        val customer = WCCustomerModel().apply {
            this.remoteCustomerId = remoteCustomerId
            this.localSiteId = site.id
            this.username = username
        }

        // when
        CustomerSqlUtils.insertOrUpdateCustomer(customer)

        // then
        val storedCustomer = CustomerSqlUtils.getCustomerByRemoteId(site, remoteCustomerId)
        assertEquals(storedCustomer!!.remoteCustomerId, remoteCustomerId)
        assertEquals(storedCustomer.localSiteId, site.id)
        assertEquals(storedCustomer.username, username)
    }

    @Test
    fun `get customer by remote id returns null when there another customer stored`() {
        // given
        val remoteCustomerId = 1L
        val username = "userName"
        val customer = WCCustomerModel().apply {
            this.remoteCustomerId = remoteCustomerId
            this.localSiteId = 3
            this.username = username
        }

        // when
        CustomerSqlUtils.insertOrUpdateCustomer(customer)

        // then
        val storedCustomer = CustomerSqlUtils.getCustomerByRemoteId(site, remoteCustomerId)
        assertNull(storedCustomer)
    }
}
