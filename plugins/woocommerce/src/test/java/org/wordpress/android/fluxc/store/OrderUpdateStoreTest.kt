package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
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
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderDto.Billing
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderDto.Shipping
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.wrappers.OrderSqlDao
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.EMPTY_BILLING_EMAIL
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.INVALID_PARAM
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderPayload
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.OptimisticUpdateResult
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.RemoteUpdateResult
import org.wordpress.android.fluxc.tools.CoroutineEngine

@ExperimentalCoroutinesApi
class OrderUpdateStoreTest {
    private lateinit var sut: OrderUpdateStore
    private lateinit var orderRestClient: OrderRestClient

    private val orderSqlDao: OrderSqlDao = mock {
        on { insertOrUpdateOrder(any()) } doReturn ROWS_AFFECTED
        on { updateLocalOrder(any(), any()) } doReturn ROWS_AFFECTED
        on { getOrderByLocalId(LocalId(TEST_LOCAL_ORDER_ID)) } doReturn initialOrder
    }

    private val siteSqlUtils: SiteSqlUtils = mock {
        on { getSiteWithLocalId(any()) } doReturn site
    }

    fun setUp(setMocks: () -> Unit) {
        setMocks.invoke()
        sut = OrderUpdateStore(
                coroutineEngine = CoroutineEngine(
                        TestCoroutineScope().coroutineContext,
                        mock()
                ),
                orderRestClient,
                orderSqlDao,
                siteSqlUtils
        )
    }

//    Updating customer notes

    @Test
    fun `should optimistically update order customer notes`(): Unit = runBlocking {
        // given
        val updatedOrder = WCOrderModel().apply {
            customerNote = UPDATED_CUSTOMER_NOTE
        }

        setUp {
            orderRestClient = mock {
                onBlocking { updateCustomerOrderNote(initialOrder, site, UPDATED_CUSTOMER_NOTE) }.doReturn(
                        RemoteOrderPayload(
                                updatedOrder,
                                site
                        )
                )
            }
        }

        // when
        val results = sut.updateCustomerOrderNote(
                orderLocalId = LocalId(initialOrder.id),
                site = site,
                newCustomerNote = UPDATED_CUSTOMER_NOTE
        ).toList()

        // then
        assertThat(results).hasSize(2).containsExactly(
                OptimisticUpdateResult(
                        OnOrderChanged(ROWS_AFFECTED)
                ),
                RemoteUpdateResult(
                        OnOrderChanged(ROWS_AFFECTED)
                )
        )
        verify(orderSqlDao).insertOrUpdateOrder(argThat {
            customerNote == UPDATED_CUSTOMER_NOTE
        })
    }

    @Test
    fun `should revert local customer notes update if remote update failed`(): Unit = runBlocking {
        // given
        setUp {
            orderRestClient = mock {
                onBlocking { updateCustomerOrderNote(initialOrder, site, UPDATED_CUSTOMER_NOTE) }.doReturn(
                        RemoteOrderPayload(
                                error = WCOrderStore.OrderError(),
                                initialOrder,
                                site
                        )
                )
            }
        }

        // when
        val results = sut.updateCustomerOrderNote(
                orderLocalId = LocalId(initialOrder.id),
                site = site,
                newCustomerNote = UPDATED_CUSTOMER_NOTE
        ).toList()

        // then
        assertThat(results).hasSize(2).containsExactly(
                OptimisticUpdateResult(
                        OnOrderChanged(ROWS_AFFECTED)
                ),
                RemoteUpdateResult(
                        OnOrderChanged(ROWS_AFFECTED)
                )
        )

        val remoteUpdateResult = results[1]
        assertThat(remoteUpdateResult.event.error.type).isEqualTo(GENERIC_ERROR)

        verify(orderSqlDao).insertOrUpdateOrder(argThat {
            customerNote == INITIAL_CUSTOMER_NOTE
        })
    }

    @Test
    fun `should emit optimistic update failure if order not found when updating status`(): Unit = runBlocking {
        // given
        setUp {
            orderRestClient = mock()
            whenever(orderSqlDao.getOrderByLocalId(any())).thenReturn(null)
        }

        // when
        val results = sut.updateCustomerOrderNote(
                orderLocalId = LocalId(initialOrder.id),
                site = site,
                newCustomerNote = UPDATED_CUSTOMER_NOTE
        ).toList()

        // then
        val remoteUpdateResult = results.first()
        SoftAssertions.assertSoftly {
            it.assertThat(remoteUpdateResult.event.error.type)
                    .isEqualTo(GENERIC_ERROR)
            it.assertThat(remoteUpdateResult.event.error.message)
                    .isEqualTo("Order with id ${initialOrder.id} not found")
        }
        verifyZeroInteractions(orderRestClient)
    }

    //    Updating addresses
    @Test
    fun `should optimistically update shipping and billing addresses`(): Unit = runBlocking {
        // given
        val updatedOrder = WCOrderModel().apply {
            shippingFirstName = UPDATED_SHIPPING_FIRST_NAME
            billingFirstName = UPDATED_BILLING_FIRST_NAME
        }

        setUp {
            orderRestClient = mock {
                onBlocking {
                    updateBothOrderAddresses(
                            initialOrder,
                            site,
                            emptyShippingDto.copy(first_name = UPDATED_SHIPPING_FIRST_NAME),
                            emptyBillingDto.copy(first_name = UPDATED_BILLING_FIRST_NAME)
                    )
                } doReturn (RemoteOrderPayload(updatedOrder, site))
            }
        }

        // when
        val results = sut.updateBothOrderAddresses(
                orderLocalId = LocalId(initialOrder.id),
                shippingAddress = emptyShipping.copy(firstName = UPDATED_SHIPPING_FIRST_NAME),
                billingAddress = emptyBilling.copy(firstName = UPDATED_BILLING_FIRST_NAME)
        ).toList()

        // then
        assertThat(results).hasSize(2).containsExactly(
                OptimisticUpdateResult(OnOrderChanged(ROWS_AFFECTED)),
                RemoteUpdateResult(OnOrderChanged(ROWS_AFFECTED))
        )
        verify(orderSqlDao).insertOrUpdateOrder(argThat {
            shippingFirstName == UPDATED_SHIPPING_FIRST_NAME &&
                    billingFirstName == UPDATED_BILLING_FIRST_NAME
        })
    }

    @Test
    fun `should optimistically update shipping address`(): Unit = runBlocking {
        // given
        val updatedOrder = WCOrderModel().apply {
            shippingFirstName = UPDATED_SHIPPING_FIRST_NAME
        }

        setUp {
            orderRestClient = mock {
                onBlocking {
                    updateShippingAddress(
                            initialOrder,
                            site,
                            emptyShippingDto.copy(first_name = UPDATED_SHIPPING_FIRST_NAME)
                    )
                } doReturn (RemoteOrderPayload(updatedOrder, site))
            }
        }

        // when
        val results = sut.updateOrderAddress(
                orderLocalId = LocalId(initialOrder.id),
                newAddress = emptyShipping.copy(firstName = UPDATED_SHIPPING_FIRST_NAME)
        ).toList()

        // then
        assertThat(results).hasSize(2).containsExactly(
                OptimisticUpdateResult(OnOrderChanged(ROWS_AFFECTED)),
                RemoteUpdateResult(OnOrderChanged(ROWS_AFFECTED))
        )
        verify(orderSqlDao).insertOrUpdateOrder(argThat {
            shippingFirstName == UPDATED_SHIPPING_FIRST_NAME
        })
    }

    @Test
    fun `should revert local shipping address update if remote update failed`(): Unit = runBlocking {
        // given
        setUp {
            orderRestClient = mock {
                onBlocking {
                    updateShippingAddress(
                            initialOrder, site, emptyShippingDto.copy(first_name = UPDATED_SHIPPING_FIRST_NAME)
                    )
                }.doReturn(
                        RemoteOrderPayload(
                                error = WCOrderStore.OrderError(),
                                initialOrder,
                                site
                        )
                )
            }
        }

        // when
        val results = sut.updateOrderAddress(
                orderLocalId = LocalId(initialOrder.id),
                newAddress = emptyShipping.copy(firstName = UPDATED_SHIPPING_FIRST_NAME)
        ).toList()

        // then
        assertThat(results).hasSize(2).containsExactly(
                OptimisticUpdateResult(OnOrderChanged(ROWS_AFFECTED)),
                RemoteUpdateResult(OnOrderChanged(ROWS_AFFECTED))
        )

        val remoteUpdateResult = results[1]
        assertThat(remoteUpdateResult.event.error.type).isEqualTo(GENERIC_ERROR)

        verify(orderSqlDao).insertOrUpdateOrder(argThat {
            shippingFirstName == INITIAL_SHIPPING_FIRST_NAME
        })
    }

    @Test
    fun `should emit optimistic update failure if order not found on updating shipping address`(): Unit = runBlocking {
        // given
        setUp {
            orderRestClient = mock()
            whenever(orderSqlDao.getOrderByLocalId(any())).thenReturn(null)
        }

        // when
        val results = sut.updateOrderAddress(
                orderLocalId = LocalId(initialOrder.id),
                newAddress = emptyShipping.copy(firstName = UPDATED_SHIPPING_FIRST_NAME)
        ).toList()

        // then
        val remoteUpdateResult = results.first()
        SoftAssertions.assertSoftly {
            it.assertThat(remoteUpdateResult.event.error.type)
                    .isEqualTo(GENERIC_ERROR)
            it.assertThat(remoteUpdateResult.event.error.message)
                    .isEqualTo("Order with id ${initialOrder.id} not found")
        }
        verifyZeroInteractions(orderRestClient)
    }

    @Test
    fun `should emit optimistic update failure if site not found on updating shipping address`(): Unit = runBlocking {
        // given
        setUp {
            orderRestClient = mock()
            whenever(siteSqlUtils.getSiteWithLocalId(any())).thenReturn(null)
        }

        // when
        val results = sut.updateOrderAddress(
                orderLocalId = LocalId(initialOrder.id),
                newAddress = emptyShipping.copy(firstName = UPDATED_SHIPPING_FIRST_NAME)
        ).toList()

        // then
        val remoteUpdateResult = results.first()
        SoftAssertions.assertSoftly {
            it.assertThat(remoteUpdateResult.event.error.type)
                    .isEqualTo(GENERIC_ERROR)
            it.assertThat(remoteUpdateResult.event.error.message)
                    .isEqualTo("Site with local id ${initialOrder.localSiteId} not found")
        }
        verifyZeroInteractions(orderRestClient)
    }

    @Test
    fun `should emit empty billing email address if its likely that that's the error`(): Unit = runBlocking {
        // given
        setUp {
            orderRestClient = mock {
                onBlocking {
                    updateBillingAddress(
                            initialOrder, site, emptyBillingDto
                    )
                }.doReturn(
                        RemoteOrderPayload(
                                error = WCOrderStore.OrderError(type = INVALID_PARAM),
                                initialOrder,
                                site
                        )
                )
            }
        }

        // when
        val results = sut.updateOrderAddress(
                orderLocalId = LocalId(initialOrder.id),
                newAddress = emptyBilling
        ).toList()

        // then
        assertThat(results[1].event.error.type).isEqualTo(EMPTY_BILLING_EMAIL)
    }

    @Test
    fun `should not emit empty billing email address if its not likely that that's the error`(): Unit = runBlocking {
        // given
        setUp {
            orderRestClient = mock {
                onBlocking {
                    updateBillingAddress(
                            initialOrder, site, emptyBillingDto.copy(email = "custom@mail.com")
                    )
                }.doReturn(
                        RemoteOrderPayload(
                                error = WCOrderStore.OrderError(type = GENERIC_ERROR),
                                initialOrder,
                                site
                        )
                )
            }
        }

        // when
        val results = sut.updateOrderAddress(
                orderLocalId = LocalId(initialOrder.id),
                newAddress = emptyBilling.copy(email = "custom@mail.com")
        ).toList()

        // then
        assertThat(results[1].event.error.type).isEqualTo(GENERIC_ERROR)
    }

    private companion object {
        const val ROWS_AFFECTED = 1
        const val TEST_LOCAL_ORDER_ID = 321
        const val TEST_LOCAL_SITE_ID = 654
        const val INITIAL_CUSTOMER_NOTE = "original customer note"
        const val UPDATED_CUSTOMER_NOTE = "updated customer note"
        const val INITIAL_SHIPPING_FIRST_NAME = "original shipping first name"
        const val UPDATED_SHIPPING_FIRST_NAME = "updated shipping first name"
        const val UPDATED_BILLING_FIRST_NAME = "updated billing first name"

        val initialOrder = WCOrderModel().apply {
            id = TEST_LOCAL_ORDER_ID
            localSiteId = TEST_LOCAL_SITE_ID
            customerNote = INITIAL_CUSTOMER_NOTE
            shippingFirstName = INITIAL_SHIPPING_FIRST_NAME
        }

        val site = SiteModel().apply {
            id = TEST_LOCAL_SITE_ID
        }

        val emptyShipping = OrderAddress.Shipping("", "", "", "", "", "", "", "", "", "")
        val emptyBilling = OrderAddress.Billing("", "", "", "", "", "", "", "", "", "", "")
        val emptyShippingDto = Shipping("", "", "", "", "", "", "", "", "", "")
        val emptyBillingDto = Billing("", "", "", "", "", "", "", "", "", "", "")
    }
}
