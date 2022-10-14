package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import org.wordpress.android.fluxc.network.Response

@Suppress("PropertyName", "VariableNaming")
class OrdersBatchDto : Response {
    val create: List<OrderDto>? = null
    val delete: List<OrderDto>? = null
    val update: List<OrderDto>? = null
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