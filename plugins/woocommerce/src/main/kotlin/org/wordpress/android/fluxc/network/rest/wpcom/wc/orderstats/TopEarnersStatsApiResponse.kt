package org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.annotations.JsonAdapter
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.network.utils.getInt
import org.wordpress.android.fluxc.network.utils.getJsonObject
import org.wordpress.android.fluxc.network.utils.getString
import java.lang.reflect.Type

@JsonAdapter(TopEarnersDeserializer::class)
class TopEarnersStatsApiResponse(
    val date: String,
    val unit: String,
    val topEarners: List<WCTopEarnerModel>
)

private class TopEarnersDeserializer : JsonDeserializer<TopEarnersStatsApiResponse> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): TopEarnersStatsApiResponse {
        val jsonObject = json.asJsonObject
        val jsonBody = jsonObject.getJsonObject("body")
        val topEarners = jsonBody?.getAsJsonArray("data")?.map { getTopEarner(it.asJsonObject) } ?: ArrayList()
        val date = jsonBody.getJsonObject("date").toString()
        val unit = jsonBody.getJsonObject("unit").toString()
        return TopEarnersStatsApiResponse(date, unit, topEarners)
    }

    private fun getTopEarner(json: JsonObject): WCTopEarnerModel {
        val model = WCTopEarnerModel()
        model.id = json.getInt("ID")
        model.currency = json.getString("currency")
        model.image = json.getString("image")
        model.name = json.getString("name", unescapeHtml4 = true)
        model.price = json.getInt("price")
        model.quantity = json.getInt("quantity")
        model.total = json.getInt("total")
        return model
    }
}
