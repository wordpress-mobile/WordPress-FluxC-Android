package org.wordpress.android.fluxc.persistence.wrappers

import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.persistence.OrderSqlUtils
import javax.inject.Inject

typealias RowAffected = Int

internal class OrderSqlDao @Inject constructor() {
    fun insertOrUpdateOrder(order: WCOrderModel): RowAffected = OrderSqlUtils.insertOrUpdateOrder(order)

    fun updateLocalOrder(localOrderId: Int, updateOrder: WCOrderModel.() -> Unit): RowAffected {
        val updatedOrder = OrderSqlUtils.getOrderByLocalId(localOrderId)
                .apply(updateOrder)
        return OrderSqlUtils.insertOrUpdateOrder(updatedOrder)
    }

    fun getOrderByLocalId(orderLocalId: LocalId): WCOrderModel? {
        return OrderSqlUtils.getOrderByLocalIdOrNull(orderLocalId)
    }
}
