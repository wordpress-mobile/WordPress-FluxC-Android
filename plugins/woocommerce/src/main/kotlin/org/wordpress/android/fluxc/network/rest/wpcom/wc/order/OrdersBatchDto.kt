package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.network.Response

@Suppress("PropertyName", "VariableNaming")
class OrdersBatchDto : Response {
    val create: List<OrderDto> = emptyList()
    val delete: List<OrderDto> = emptyList()
    val update: List<OrderDto> = emptyList()
}

class OrdersBatchUpdateRequest {
    companion object {
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
}

data class OrdersDatabaseBatch (
    val createdEntities: List<OrderEntity>,
    val updatedEntities: List<OrderEntity>,
    val deletedEntities: List<OrderEntity>
)