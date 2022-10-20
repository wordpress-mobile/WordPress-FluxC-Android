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
    ) = buildMap {
        putNotEmpty("create", createRequest)
        putNotEmpty("update", updateRequest)
        putNotEmpty("delete", deleteRequest)
    }

    private fun MutableMap<String, Any>.putNotEmpty(key: String, value: List<*>) {
        if (value.isNotEmpty()) this[key] = value
    }
}

data class OrdersDatabaseBatch(
    val createdEntities: List<OrderEntity>,
    val updatedEntities: List<OrderEntity>,
    val deletedEntities: List<OrderEntity>
)
