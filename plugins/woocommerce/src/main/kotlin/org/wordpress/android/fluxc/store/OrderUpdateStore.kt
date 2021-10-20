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
import org.wordpress.android.fluxc.persistence.wrappers.OrderSqlDao
import org.wordpress.android.fluxc.persistence.wrappers.RowsAffected
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
    private val orderSqlDao: OrderSqlDao,
    private val siteSqlUtils: SiteSqlUtils
) {
    suspend fun updateCustomerOrderNote(
        orderLocalId: LocalId,
            // todo wzieba: Remove site argument. As we have local order id we can get site from database. No need to bother client with that.
        site: SiteModel,
        newCustomerNote: String
    ): Flow<UpdateOrderResult> {
        return coroutineEngine.flowWithDefaultContext(T.API, this, "updateCustomerOrderNote") {
            val initialOrder = orderSqlDao.getOrderByLocalId(orderLocalId)

            if (initialOrder == null) {
                emitNoEntityFound("Order with id ${orderLocalId.value} not found")
            } else {
                val optimisticUpdateRowsAffected: RowsAffected = orderSqlDao.updateLocalOrder(initialOrder.id) {
                    customerNote = newCustomerNote
                }
                emit(UpdateOrderResult.OptimisticUpdateResult(OnOrderChanged(optimisticUpdateRowsAffected)))

                val updateRemoteOrderPayload = wcOrderRestClient.updateCustomerOrderNote(
                        initialOrder,
                        site,
                        newCustomerNote
                )

                emitRemoteUpdateResultOrRevertOnError(updateRemoteOrderPayload, initialOrder)
            }
        }
    }

    suspend fun updateOrderAddress(
        orderLocalId: LocalId,
        newAddress: OrderAddress
    ): Flow<UpdateOrderResult> {
        return coroutineEngine.flowWithDefaultContext(T.API, this, "updateOrderAddress") {
            takeWhenOrderDataAcquired(orderLocalId) { initialOrder, site ->
                val optimisticUpdateRowsAffected: RowsAffected = updateLocalOrderAddress(initialOrder, newAddress)
                emit(UpdateOrderResult.OptimisticUpdateResult(OnOrderChanged(optimisticUpdateRowsAffected)))

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
                ).let { emit(UpdateOrderResult.OptimisticUpdateResult(OnOrderChanged(it))) }

                wcOrderRestClient.updateAllAddresses(
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
        orderSqlDao.getOrderByLocalId(orderLocalId)?.let { initialOrder ->
            siteSqlUtils.getSiteWithLocalId(LocalId(initialOrder.localSiteId))
                    ?.let { predicate(initialOrder, it) }
                    ?: emitNoEntityFound("Site with local id ${initialOrder.localSiteId} not found")
        } ?: emitNoEntityFound("Order with id ${orderLocalId.value} not found")
    }

    private fun updateLocalOrderAddress(
        initialOrder: WCOrderModel,
        newAddress: OrderAddress
    ) = orderSqlDao.updateLocalOrder(initialOrder.id) {
        when (newAddress) {
            is Billing -> updateLocalBillingAddress(newAddress)
            is Shipping -> updateLocalShippingAddress(newAddress)
        }
    }

    private fun updateBothLocalOrderAddresses(
        initialOrder: WCOrderModel,
        shippingAddress: Shipping,
        billingAddress: Billing
    ) = orderSqlDao.updateLocalOrder(initialOrder.id) {
        updateLocalShippingAddress(shippingAddress)
        updateLocalBillingAddress(billingAddress)
    }

    private fun WCOrderModel.updateLocalShippingAddress(newAddress: OrderAddress) {
        this.shippingFirstName = newAddress.firstName
        this.shippingLastName = newAddress.lastName
        this.shippingCompany = newAddress.company
        this.shippingAddress1 = newAddress.address1
        this.shippingAddress2 = newAddress.address2
        this.shippingCity = newAddress.city
        this.shippingState = newAddress.state
        this.shippingPostcode = newAddress.postcode
        this.shippingCountry = newAddress.country
    }

    private fun WCOrderModel.updateLocalBillingAddress(newAddress: Billing) {
        this.billingFirstName = newAddress.firstName
        this.billingLastName = newAddress.lastName
        this.billingCompany = newAddress.company
        this.billingAddress1 = newAddress.address1
        this.billingAddress2 = newAddress.address2
        this.billingCity = newAddress.city
        this.billingState = newAddress.state
        this.billingPostcode = newAddress.postcode
        this.billingCountry = newAddress.country
        this.billingEmail = newAddress.email
        this.billingPhone = newAddress.phone
    }

    private suspend fun FlowCollector<UpdateOrderResult>.emitRemoteUpdateResultOrRevertOnError(
        updateRemoteOrderPayload: RemoteOrderPayload,
        initialOrder: WCOrderModel
    ) {
        val remoteUpdateResult = if (updateRemoteOrderPayload.isError) {
            OnOrderChanged(orderSqlDao.insertOrUpdateOrder(initialOrder)).apply {
                error = updateRemoteOrderPayload.error
            }
        } else {
            OnOrderChanged(orderSqlDao.insertOrUpdateOrder(updateRemoteOrderPayload.order))
        }

        emit(RemoteUpdateResult(remoteUpdateResult))
    }

    private suspend fun FlowCollector<UpdateOrderResult>.emitNoEntityFound(message: String) {
        emit(UpdateOrderResult.OptimisticUpdateResult(
                OnOrderChanged(NO_ROWS_AFFECTED).apply {
                    error = WCOrderStore.OrderError(message = message)
                }
        ))
    }

    private companion object {
        const val NO_ROWS_AFFECTED = 0
    }
}
