package org.wordpress.android.fluxc.model.order

import org.wordpress.android.fluxc.model.WCOrderModel

typealias OrderIdentifier = String

data class OrderIdSet(val id: Int, val remoteOrderId: Long, val localSiteId: Int)

@Suppress("FunctionName")
fun OrderIdentifier(orderModel: WCOrderModel): OrderIdentifier {
    return with(orderModel) { "$id-$remoteOrderId-$localSiteId" }
}

@Suppress("FunctionName")
fun OrderIdentifier(localSiteId: Int, remoteOrderId: Long): OrderIdentifier {
    return "0-$remoteOrderId-$localSiteId"
}

@Suppress("FunctionName")
fun OrderIdentifier(localOrderId: Int, localSiteId: Int, remoteOrderId: Long): OrderIdentifier {
    return "$localOrderId-$remoteOrderId-$localSiteId"
}

fun OrderIdentifier.toIdSet(): OrderIdSet {
    val (id, remoteOrderId, localSiteId) = split("-")
    return OrderIdSet(id.toInt(), remoteOrderId.toLong(), localSiteId.toInt())
}
