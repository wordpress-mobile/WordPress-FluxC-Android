package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.network.Response

@Suppress("PropertyName", "VariableNaming")
data class OrdersBatchDto(
    val create: List<OrderDto>? = null,
    val delete: List<OrderDto>? = null,
    val update: List<OrderDto>? = null
) : Response

object OrdersBatchUpdateRequest {
    fun buildBody(
        createRequest: List<Map<String, Any>>,
        updateRequest: List<Map<String, Any>>,
        deleteRequest: List<Long>
    ) = mutableMapOf<String, Any>().apply {
        put("create", createRequest)
        put("update", updateRequest)
        put("delete", deleteRequest)
    }
}

data class OrdersDatabaseBatch(
    val createdEntities: List<OrderEntity>,
    val updatedEntities: List<OrderEntity>,
    val deletedEntities: List<OrderEntity>
)
