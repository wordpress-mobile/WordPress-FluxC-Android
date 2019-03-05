package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.JsonElement
import org.wordpress.android.fluxc.network.Response

@Suppress("PropertyName")
class ProductVariationApiResponse : Response {
    var id: Long = 0L

    var description: String? = null
    var permalink: String? = null
    var sku: String? = null
    var status: String? = null
    var price: String? = null
    var regular_price: String? = null
    var sale_price: String? = null

    var on_sale = false
    var purchasable = false
    var virtual = false
    var downloadable = false

    var manage_stock = false
    var stock_quantity = 0
    var stock_status: String? = null

    var image: JsonElement? = null

    var weight: String? = null
    var dimensions: JsonElement? = null
    var attributes: JsonElement? = null
}
