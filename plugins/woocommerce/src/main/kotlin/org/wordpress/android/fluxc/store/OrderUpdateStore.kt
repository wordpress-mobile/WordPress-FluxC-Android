package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.model.order.OrderAddress.Billing
import org.wordpress.android.fluxc.model.order.OrderAddress.Shipping
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderDtoMapper.toDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.dao.OrdersDao
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderPayload
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.RemoteUpdateResult
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton

typealias UpdateOrderFlowPredicate = suspend FlowCollector<UpdateOrderResult>.(WCOrderModel, SiteModel) -> Unit

@Singleton
class OrderUpdateStore @Inject internal constructor(
    private val coroutineEngine: CoroutineEngine,
    private val wcOrderRestClient: OrderRestClient,
    private val ordersDao: OrdersDao,
    private val siteSqlUtils: SiteSqlUtils
) {
    suspend fun updateCustomerOrderNote(
        orderLocalId: LocalId,
            // todo wzieba: Remove site argument. As we have local order id we can get site from database. No need to bother client with that.
        site: SiteModel,
        newCustomerNote: String
    ): Flow<UpdateOrderResult> {
        return coroutineEngine.flowWithDefaultContext(T.API, this, "updateCustomerOrderNote") {
            val initialOrder = ordersDao.getOrderByLocalId(orderLocalId)

            if (initialOrder == null) {
                emitNoEntityFound("Order with id ${orderLocalId.value} not found")
            } else {
                ordersDao.updateLocalOrder(initialOrder.id) {
                    copy(customerNote = newCustomerNote)
                }
                emit(UpdateOrderResult.OptimisticUpdateResult(OnOrderChanged()))

                val updateRemoteOrderPayload = wcOrderRestClient.updateCustomerOrderNote(
                        initialOrder,
                        site,
                        newCustomerNote
                )
                val remoteUpdateResult = if (updateRemoteOrderPayload.isError) {
                    ordersDao.insertOrUpdateOrder(initialOrder)
                    OnOrderChanged(orderError = updateRemoteOrderPayload.error)
                } else {
                    ordersDao.insertOrUpdateOrder(updateRemoteOrderPayload.order)
                    OnOrderChanged()
                }
                emit(RemoteUpdateResult(remoteUpdateResult))
            }
        }
    }

    suspend fun updateOrderAddress(
        orderLocalId: LocalId,
        newAddress: OrderAddress
    ): Flow<UpdateOrderResult> {
        return coroutineEngine.flowWithDefaultContext(T.API, this, "updateOrderAddress") {
            takeWhenOrderDataAcquired(orderLocalId) { initialOrder, site ->
                updateLocalOrderAddress(initialOrder, newAddress)
                emit(UpdateOrderResult.OptimisticUpdateResult(OnOrderChanged()))

                val updateRemoteOrderPayload = when (newAddress) {
                    is Billing -> wcOrderRestClient.updateBillingAddress(initialOrder, site, newAddress.toDto())
                    is Shipping -> wcOrderRestClient.updateShippingAddress(initialOrder, site, newAddress.toDto())
                }

                emitRemoteUpdateResultOrRevertOnError(updateRemoteOrderPayload, initialOrder)
            }
        }
    }

    suspend fun updateBothOrderAddresses(
        orderLocalId: LocalId,
        shippingAddress: Shipping,
        billingAddress: Billing
    ): Flow<UpdateOrderResult> {
        return coroutineEngine.flowWithDefaultContext(T.API, this, "updateBothOrderAddresses") {
            takeWhenOrderDataAcquired(orderLocalId) { initialOrder, site ->
                updateBothLocalOrderAddresses(
                        initialOrder,
                        shippingAddress,
                        billingAddress
                ).let { emit(UpdateOrderResult.OptimisticUpdateResult(OnOrderChanged())) }

                wcOrderRestClient.updateBothOrderAddresses(
                        initialOrder,
                        site,
                        shippingAddress.toDto(),
                        billingAddress.toDto()
                ).let { emitRemoteUpdateResultOrRevertOnError(it, initialOrder) }
            }
        }
    }

    private suspend fun FlowCollector<UpdateOrderResult>.takeWhenOrderDataAcquired(
        orderLocalId: LocalId,
        predicate: UpdateOrderFlowPredicate
    ) {
        ordersDao.getOrderByLocalId(orderLocalId)?.let { initialOrder ->
            siteSqlUtils.getSiteWithLocalId(LocalId(initialOrder.localSiteId))
                    ?.let { predicate(initialOrder, it) }
                    ?: emitNoEntityFound("Site with local id ${initialOrder.localSiteId} not found")
        } ?: emitNoEntityFound("Order with id ${orderLocalId.value} not found")
    }

    private fun updateLocalOrderAddress(
        initialOrder: WCOrderModel,
        newAddress: OrderAddress
    ) = ordersDao.updateLocalOrder(initialOrder.id) {
        when (newAddress) {
            is Billing -> updateLocalBillingAddress(newAddress)
            is Shipping -> updateLocalShippingAddress(newAddress)
        }
    }

    private fun updateBothLocalOrderAddresses(
        initialOrder: WCOrderModel,
        shippingAddress: Shipping,
        billingAddress: Billing
    ) = ordersDao.updateLocalOrder(initialOrder.id) {
        updateLocalShippingAddress(shippingAddress)
        updateLocalBillingAddress(billingAddress)
    }

    private fun WCOrderModel.updateLocalShippingAddress(newAddress: OrderAddress): WCOrderModel {
        return copy(
                shippingFirstName = newAddress.firstName,
                shippingLastName = newAddress.lastName,
                shippingCompany = newAddress.company,
                shippingAddress1 = newAddress.address1,
                shippingAddress2 = newAddress.address2,
                shippingCity = newAddress.city,
                shippingState = newAddress.state,
                shippingPostcode = newAddress.postcode,
                shippingCountry = newAddress.country,
                shippingPhone = newAddress.phone
        )
    }

    private fun WCOrderModel.updateLocalBillingAddress(newAddress: Billing): WCOrderModel {
        return copy(
                billingFirstName = newAddress.firstName,
                billingLastName = newAddress.lastName,
                billingCompany = newAddress.company,
                billingAddress1 = newAddress.address1,
                billingAddress2 = newAddress.address2,
                billingCity = newAddress.city,
                billingState = newAddress.state,
                billingPostcode = newAddress.postcode,
                billingCountry = newAddress.country,
                billingEmail = newAddress.email,
                billingPhone = newAddress.phone
        )
    }

    private suspend fun FlowCollector<UpdateOrderResult>.emitRemoteUpdateResultOrRevertOnError(
        updateRemoteOrderPayload: RemoteOrderPayload,
        initialOrder: WCOrderModel
    ) {
        val remoteUpdateResult = if (updateRemoteOrderPayload.isError) {
            ordersDao.insertOrUpdateOrder(initialOrder)
            OnOrderChanged(orderError = updateRemoteOrderPayload.error)
        } else {
            ordersDao.insertOrUpdateOrder(updateRemoteOrderPayload.order)
            OnOrderChanged()
        }

        emit(RemoteUpdateResult(remoteUpdateResult))
    }

    private suspend fun FlowCollector<UpdateOrderResult>.emitNoEntityFound(message: String) {
        emit(UpdateOrderResult.OptimisticUpdateResult(
                OnOrderChanged(orderError = WCOrderStore.OrderError(message = message))
        ))
    }
}
