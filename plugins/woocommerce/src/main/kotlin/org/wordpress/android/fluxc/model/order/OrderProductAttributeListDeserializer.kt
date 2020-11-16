package org.wordpress.android.fluxc.model.order

import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderModel.LineItem.Attribute
import java.lang.reflect.Type

/**
 * Custom deserializer to parse the attributes from [WCOrderModel.LineItem.attributes] to return a list
 * of [Attribute]. This was necessary to prevent issues with plugins adding unexpected content such as json
 * arrays as the value when a String is expected.
 */
class OrderProductAttributeListDeserializer : JsonDeserializer<List<Attribute>> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): List<Attribute> {
        val result = mutableListOf<Attribute>()

        json as JsonArray
        for (item: JsonElement in json) {
            if (item.isJsonObject) {
                item as JsonObject

                if (item.has("display_key") && item.get("display_key").isJsonPrimitive &&
                        item.has("display_value") && item.get("display_value").isJsonPrimitive) {
                    val key = item.get("display_key").asString.trim()
                    val value = item.get("display_value").asString.trim()
                    result.add(Attribute(key, value))
                }
            }
        }

        return result
    }
}
