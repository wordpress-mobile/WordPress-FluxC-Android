package org.wordpress.android.fluxc.model.order.entities

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.LineItemDto
import javax.inject.Inject

class LineItemSerializer @Inject constructor(private val gson: Gson) {
    fun mapLineItems(
        order: WCOrderModel,
        rawLineItemList: JsonElement
    ): List<LineItemEntity> {
        val responseType = object : TypeToken<List<LineItemDto>>() {}.type
        return (gson.fromJson(rawLineItemList.toString(), responseType) as? List<LineItemDto>)?.map { lineItemDto ->
            LineItemEntity(
                    parentOrderId = order.remoteOrderId.value,
                    parentOrderLocalSiteId = order.localSiteId.value.toLong(),
                    id = lineItemDto.id,
                    name = lineItemDto.name,
                    parentName = lineItemDto.parentName,
                    productId = lineItemDto.productId,
                    variationId = lineItemDto.variationId,
                    quantity = lineItemDto.quantity,
                    subtotal = lineItemDto.subtotal,
                    total = lineItemDto.total,
                    totalTax = lineItemDto.totalTax,
                    sku = lineItemDto.sku,
                    price = lineItemDto.price,
            )
        }.orEmpty()
    }
}
