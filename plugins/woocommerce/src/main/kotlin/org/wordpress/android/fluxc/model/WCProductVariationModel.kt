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
    var virtual = false

    var downloadable = false
    var downloadLimit = 0
    var downloadExpiry = 0

    var taxStatus = ""
    var taxClass = ""

    var backorders = ""
    var backordersAllowed = false
    var backordered = false

    var shippingClass = ""
    var shippingClassId = 0

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
