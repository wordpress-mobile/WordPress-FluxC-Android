package org.wordpress.android.fluxc.store

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.customer.WCCustomerMapper
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerApiResponse
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCCustomerStoreTest {
    private val restClient: CustomerRestClient = mock()
    private val mapper: WCCustomerMapper = mock()

    private lateinit var store: WCCustomerStore

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

        store = WCCustomerStore(
                restClient,
                initCoroutineEngine(),
                mapper
        )
    }

    @Test
    fun `fetch single customer with success returns success`() = test {
        // given
        val siteModelId = 1
        val remoteCustomerId = 2L
        val siteModel = SiteModel().apply { id = siteModelId }

        val response: CustomerApiResponse = mock()
        whenever(restClient.fetchSingleCustomer(siteModel, remoteCustomerId))
                .thenReturn(WooPayload(response))
        val model: WCCustomerModel = mock()
        whenever(mapper.map(siteModel, response)).thenReturn(model)

        // when
        val result = store.fetchSingleCustomer(siteModel, remoteCustomerId)

        // then
        assertFalse(result.isError)
        assertEquals(result.model, model)
    }

    @Test
    fun `fetch single customer with error returns error`() = test {
        // given
        val siteModelId = 1
        val remoteCustomerId = 2L
        val siteModel = SiteModel().apply { id = siteModelId }

        val error = WooError(INVALID_RESPONSE, NETWORK_ERROR, "Invalid site ID")
        whenever(restClient.fetchSingleCustomer(siteModel, remoteCustomerId)).thenReturn(WooPayload(error))

        // when
        val result = store.fetchSingleCustomer(siteModel, remoteCustomerId)

        // then
        assertTrue(result.isError)
        assertEquals(result.error, error)
    }
}
