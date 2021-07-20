package org.wordpress.android.fluxc.network.rest.wpcom.wc.addons

import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.ProductAddonApiResponse.AddOnPriceType

class ProductAddonOptionApiResponse : Response {
    val label: String? = null
    val price: String? = null
    val priceType: AddOnPriceType? = null
    val imageID: Long? = null
}