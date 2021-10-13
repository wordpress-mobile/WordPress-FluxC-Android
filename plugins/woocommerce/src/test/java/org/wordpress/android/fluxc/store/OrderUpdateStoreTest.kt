package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Test
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
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
        val updatedOrder = WCOrderModel().apply {
            customerNote = UPDATED_NOTE
        }

        setUp {
            orderRestClient = mock {
                onBlocking { updateCustomerOrderNote(initialOrder, site, UPDATED_NOTE) }.doReturn(
                        RemoteOrderPayload(
                                updatedOrder,
                                site
                        )
                )
            }
            whenever(orderSqlDao.getOrderByLocalId(LocalId(TEST_LOCAL_ORDER_ID))).thenReturn(
                    initialOrder
            )
        }

        // when
        val results = sut.updateCustomerOrderNote(
                orderLocalId = LocalId(initialOrder.id),
                site = site,
                newCustomerNote = UPDATED_NOTE
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
            customerNote == UPDATED_NOTE
        })
    }

    @Test
    fun `should revert local customer notes update if remote update failed`(): Unit = runBlocking {
        // given
        setUp {
            orderRestClient = mock {
                onBlocking { updateCustomerOrderNote(initialOrder, site, UPDATED_NOTE) }.doReturn(
                        RemoteOrderPayload(
                                error = WCOrderStore.OrderError(),
                                initialOrder,
                                site
                        )
                )
            }
            whenever(orderSqlDao.getOrderByLocalId(LocalId(TEST_LOCAL_ORDER_ID))).thenReturn(
                    initialOrder
            )
        }

        // when
        val results = sut.updateCustomerOrderNote(
                orderLocalId = LocalId(initialOrder.id),
                site = site,
                newCustomerNote = UPDATED_NOTE
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
            customerNote == INITIAL_NOTE
        })
    }

    @Test
    fun `should emit optimistic update failure if order not found`(): Unit = runBlocking {
        // given
        setUp {
            orderRestClient = mock()
            whenever(orderSqlDao.getOrderByLocalId(any())).thenReturn(null)
        }

        // when
        val results = sut.updateCustomerOrderNote(
                orderLocalId = LocalId(initialOrder.id),
                site = site,
                newCustomerNote = UPDATED_NOTE
        ).toList()

        // then
        val remoteUpdateResult = results.first()
        SoftAssertions.assertSoftly {
            it.assertThat(remoteUpdateResult.event.error.type)
                    .isEqualTo(GENERIC_ERROR)
            it.assertThat(remoteUpdateResult.event.error.message)
                    .isEqualTo("Order with id ${initialOrder.id} not found")
        }
    }

    private companion object {
        const val ROW_AFFECTED = 1
        const val TEST_LOCAL_ORDER_ID = 321
        const val TEST_LOCAL_SITE_ID = 654
        const val INITIAL_NOTE = "original customer note"
        const val UPDATED_NOTE = "updated customer note"

        val initialOrder = WCOrderModel().apply {
            id = TEST_LOCAL_ORDER_ID
            customerNote = INITIAL_NOTE
        }

        val site = SiteModel().apply {
            id = TEST_LOCAL_SITE_ID
        }
    }
}
