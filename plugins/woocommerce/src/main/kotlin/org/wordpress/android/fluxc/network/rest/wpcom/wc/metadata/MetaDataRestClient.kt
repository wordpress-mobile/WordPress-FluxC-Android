package org.wordpress.android.fluxc.network.rest.wpcom.wc.metadata

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.metadata.MetaDataParentItemType.ORDER
import org.wordpress.android.fluxc.model.metadata.MetaDataParentItemType.PRODUCT
import org.wordpress.android.fluxc.model.metadata.UpdateMetadataRequest
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload

internal class MetaDataRestClient internal constructor(
    private val wooNetwork: WooNetwork
) {
    suspend fun updateMetaData(
        site: SiteModel,
        request: UpdateMetadataRequest
    ): WooPayload<List<WCMetaData>> {
        val path = when (request.parentItemType) {
            ORDER -> WOOCOMMERCE.orders.id(request.parentItemId).pathV3
            PRODUCT -> WOOCOMMERCE.products.id(request.parentItemId).pathV3
        }

        val metaDataJson = JsonArray()
        request.insertedMetadata.forEach {
            metaDataJson.add(
                JsonObject().apply {
                    addProperty(WCMetaData.KEY, it.key)
                    add(WCMetaData.VALUE, it.value.jsonValue)
                }
            )
        }
        request.updatedMetadata.forEach {
            metaDataJson.add(it.toJson())
        }
        request.deletedMetadata.forEach {
            metaDataJson.add(
                JsonObject().apply {
                    addProperty(WCMetaData.ID, it.id)
                    add(WCMetaData.VALUE, JsonNull.INSTANCE)
                }
            )
        }

        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = path,
            body = mapOf(
                "meta_data" to metaDataJson
            ),
            clazz = JsonObject::class.java
        )

        return response.toWooPayload {
            it.getAsJsonArray("meta_data").mapNotNull {
                WCMetaData.fromJson(it)
            }
        }
    }
}
