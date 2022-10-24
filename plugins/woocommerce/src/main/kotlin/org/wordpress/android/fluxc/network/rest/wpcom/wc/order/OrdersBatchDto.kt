package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.utils.putIfNotEmpty

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
    ) = buildMap {
        putIfNotEmpty("create", createRequest)
        putIfNotEmpty("update", updateRequest)
        putIfNotEmpty("delete", deleteRequest)
    }
}

data class OrdersDatabaseBatch(
    val createdEntities: List<OrderEntity>,
    val updatedEntities: List<OrderEntity>,
    val deletedEntities: List<OrderEntity>
)
