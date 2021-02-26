package org.wordpress.android.fluxc.store

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.action.WCCustomerAction.FETCH_SINGLE_CUSTOMER
import org.wordpress.android.fluxc.generated.WCCustomerActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCCustomerModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.error.CustomerError
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCCustomerStore.FetchSingleCustomerPayload
import org.wordpress.android.fluxc.store.WCCustomerStore.OnCustomerChanged
import org.wordpress.android.fluxc.store.WCCustomerStore.RemoteCustomerPayload
import kotlin.test.assertEquals

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCCustomerStoreTest {
    private val restClient: CustomerRestClient = mock()
    private val dispatcher: Dispatcher = mock()
    private val productStore = WCCustomerStore(dispatcher, restClient)

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
    fun `on action with fetch single customer fetches data from rest client`() {
        // given
        val siteModelId = 1
        val remoteCustomerId = 2L
        val siteModel = SiteModel().apply { id = siteModelId }
        val payload = FetchSingleCustomerPayload(siteModel, remoteCustomerId)

        // when
        productStore.onAction(WCCustomerActionBuilder.newFetchSingleCustomerAction(payload))

        // then
        verify(restClient).fetchSingleCustomer(siteModel, remoteCustomerId)
    }

    @Test
    fun `on action with fetched single customer success emits on change event`() {
        // given
        val siteModelId = 1
        val remoteCustomerId = 2L
        val siteModel = SiteModel().apply { id = siteModelId }
        val customer: WCCustomerModel = mock {
            on { this.remoteCustomerId }.thenReturn(remoteCustomerId)
        }
        val payload = RemoteCustomerPayload(customer, siteModel)

        // when
        productStore.onAction(WCCustomerActionBuilder.newFetchedSingleCustomerAction(payload))

        // then
        val changeCaptor = argumentCaptor<OnCustomerChanged>()
        verify(dispatcher).emitChange(changeCaptor.capture())
        assertEquals(changeCaptor.firstValue.remoteCustomerId, remoteCustomerId)
        assertEquals(changeCaptor.firstValue.canLoadMore, false)
        assertEquals(changeCaptor.firstValue.rowsAffected, 1)
        assertEquals(changeCaptor.firstValue.causeOfChange, FETCH_SINGLE_CUSTOMER)
    }

    @Test
    fun `on action with fetched single customer error emits on change event`() {
        // given
        val siteModelId = 1
        val remoteCustomerId = 2L
        val siteModel = SiteModel().apply { id = siteModelId }
        val customer: WCCustomerModel = mock {
            on { this.remoteCustomerId }.thenReturn(remoteCustomerId)
        }
        val error = CustomerError()
        val payload = RemoteCustomerPayload(error, customer, siteModel)

        // when
        productStore.onAction(WCCustomerActionBuilder.newFetchedSingleCustomerAction(payload))

        // then
        val changeCaptor = argumentCaptor<OnCustomerChanged>()
        verify(dispatcher).emitChange(changeCaptor.capture())
        assertEquals(changeCaptor.firstValue.remoteCustomerId, remoteCustomerId)
        assertEquals(changeCaptor.firstValue.canLoadMore, false)
        assertEquals(changeCaptor.firstValue.rowsAffected, 0)
        assertEquals(changeCaptor.firstValue.causeOfChange, FETCH_SINGLE_CUSTOMER)
        assertEquals(changeCaptor.firstValue.error, error)
    }
}
