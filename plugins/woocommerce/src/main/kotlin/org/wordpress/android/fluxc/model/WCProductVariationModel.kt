package org.wordpress.android.fluxc.model

/**
 * Product variations - see http://woocommerce.github.io/woocommerce-rest-api-docs/#product-variations
 * As with WCProductModel, the backend returns more properties than are supported below
 */
data class WCProductVariationModel(
    val localSiteId: Int,
    val remoteProductId: Long
) {
    var remoteVariationId: Long = 0L

    var dateCreated = ""
    var dateModified = ""

    var description = ""
    var permalink = ""
    var sku = ""
    var status = ""

    var price = ""
    var regularPrice = ""
    var salePrice = ""

    var onSale = false
    var purchasable = false
    var downloadable = false
    var virtual = false

    var manageStock = false
    var stockQuantity = 0
    var stockStatus = ""

    var imageUrl = ""

    var weight = ""
    var length = ""
    var width = ""
    var height = ""

    var attributes = ""
}
