package org.wordpress.android.fluxc.persistence.mappers

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.network.utils.getInt
import org.wordpress.android.fluxc.network.utils.getLong
import org.wordpress.android.fluxc.network.utils.getString
import org.wordpress.android.fluxc.persistence.entity.BundledProductEntity

object BundledProductMapper {
    private const val BUNDLED_ITEM_ID = "bundled_item_id"
    private const val PRODUCT_ID = "product_id"
    private const val MENU_ORDER = "menu_order"
    private const val TITLE = "title"
    private const val STOCK_STATUS = "stock_status"

    private fun toDatabaseEntity(localSiteId: Int, productId: Long, jsonObject: JsonObject): BundledProductEntity {
        return BundledProductEntity(
            localSiteId = LocalOrRemoteId.LocalId(localSiteId),
            productId = LocalOrRemoteId.RemoteId(productId),
            bundledItemId = jsonObject.getLong(BUNDLED_ITEM_ID),
            bundledProductId = LocalOrRemoteId.RemoteId(jsonObject.getLong(PRODUCT_ID)),
            menuOrder = jsonObject.getInt(MENU_ORDER),
            title = jsonObject.getString(TITLE) ?: "",
            stockStatus = jsonObject.getString(STOCK_STATUS) ?: ""
        )
    }

    fun toDatabaseEntityList(localSiteId: Int, productId: Long?, jsonArray: JsonArray?): List<BundledProductEntity>? {
        if (jsonArray == null || productId == null) return null
        return jsonArray.asSequence()
            .mapNotNull { item -> item as? JsonObject }
            .map { item -> toDatabaseEntity(localSiteId, productId, item) }
            .toList()
    }
}
