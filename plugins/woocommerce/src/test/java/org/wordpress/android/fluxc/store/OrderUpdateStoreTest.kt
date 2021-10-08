package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.persistence.wrappers.OrderSqlDao
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderPayload
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.OptimisticUpdateResult
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.RemoteUpdateResult
import org.wordpress.android.fluxc.tools.CoroutineEngine

@ExperimentalCoroutinesApi
class OrderUpdateStoreTest {
    private lateinit var sut: OrderUpdateStore
    private lateinit var orderRestClient: OrderRestClient

    private val orderSqlDao: OrderSqlDao = mock {
        on { insertOrUpdateOrder(any()) } doReturn ROW_AFFECTED
        on { updateLocalOrder(any(), any()) } doReturn ROW_AFFECTED
    }

    fun setUp(setMocks: () -> Unit) {
        setMocks.invoke()
        sut = OrderUpdateStore(
                coroutineEngine = CoroutineEngine(
                        TestCoroutineScope().coroutineContext,
                        mock()
                ),
                orderRestClient,
                orderSqlDao
        )
    }

    @Test
    fun `should optimistically update order customer notes`(): Unit = runBlocking {
        // given
        val initialOrder = WCOrderModel().apply {
            customerNote = "original customer note"
        }
        val updatedNote = "updated customer note"
        val updatedOrder = WCOrderModel().apply {
            customerNote = updatedNote
        }
        val site = SiteModel()

        setUp {
            orderRestClient = mock {
                onBlocking { updateCustomerOrderNote(initialOrder, site, updatedNote) }.doReturn(
                        RemoteOrderPayload(
                                updatedOrder,
                                site
                        )
                )
            }
        }

        // when
        val results = sut.updateOrderNotes(
                initialOrder = initialOrder,
                site = site,
                newNotes = updatedNote
        ).toList()

        // then
        assertThat(results).hasSize(2).containsExactly(
                OptimisticUpdateResult(
                        OnOrderChanged(ROW_AFFECTED)
                ),
                RemoteUpdateResult(
                        OnOrderChanged(ROW_AFFECTED)
                )
        )
        verify(orderSqlDao).insertOrUpdateOrder(argThat {
            customerNote == updatedNote
        })
    }

    @Test
    fun `should revert local customer notes update if remote update failed`(): Unit = runBlocking {
        // given
        val originalNote = "original customer note"
        val initialOrder = WCOrderModel().apply {
            customerNote = originalNote
        }
        val updatedNote = "updated customer note"
        val site = SiteModel()

        setUp {
            orderRestClient = mock {
                onBlocking { updateCustomerOrderNote(initialOrder, site, updatedNote) }.doReturn(
                        RemoteOrderPayload(
                                error = WCOrderStore.OrderError(),
                                initialOrder,
                                site
                        )
                )
            }
        }

        // when
        val results = sut.updateOrderNotes(
                initialOrder = initialOrder,
                site = site,
                newNotes = updatedNote
        ).toList()

        // then
        assertThat(results).hasSize(2).containsExactly(
                OptimisticUpdateResult(
                        OnOrderChanged(ROW_AFFECTED)
                ),
                RemoteUpdateResult(
                        OnOrderChanged(ROW_AFFECTED)
                )
        )

        val remoteUpdateResult = results[1]
        assertThat(remoteUpdateResult.event.error.type).isEqualTo(GENERIC_ERROR)

        verify(orderSqlDao).insertOrUpdateOrder(argThat {
            customerNote == originalNote
        })
    }

    private companion object {
        const val ROW_AFFECTED = 1
    }
}
