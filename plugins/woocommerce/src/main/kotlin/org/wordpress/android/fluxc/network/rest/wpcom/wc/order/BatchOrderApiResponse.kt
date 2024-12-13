package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type
import org.wordpress.android.fluxc.network.Response

data class BatchOrderApiResponse(
    val update: List<OrderResponse>
) : Response {
    @JsonAdapter(OrderResponseDeserializer::class)
    sealed class OrderResponse {
        data class Success(
            val order: OrderDto
        ) : OrderResponse()

        data class Error(
            val id: Long,
            val error: ErrorResponse
        ) : OrderResponse()
    }

    data class ErrorResponse(
        val code: String,
        val message: String,
        val data: ErrorData
    )

    data class ErrorData(
        val status: Int
    )

    private class OrderResponseDeserializer : JsonDeserializer<OrderResponse> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): OrderResponse {
            val jsonObject = json.asJsonObject

            return if (jsonObject.has("error")) {
                OrderResponse.Error(
                    id = jsonObject.get("id").asLong,
                    error = context.deserialize(jsonObject.get("error"), ErrorResponse::class.java)
                )
            } else {
                OrderResponse.Success(
                    context.deserialize(jsonObject, OrderDto::class.java)
                )
            }
        }
    }
}
