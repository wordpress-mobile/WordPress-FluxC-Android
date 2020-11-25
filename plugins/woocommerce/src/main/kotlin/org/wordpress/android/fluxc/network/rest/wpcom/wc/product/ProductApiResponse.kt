package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.JsonElement
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.utils.getString

@Suppress("PropertyName")
class ProductApiResponse : Response {
    val id: Long? = null
    var localSiteId = 0
    var name: String? = null
    var slug: String? = null
    var permalink: String? = null

    var date_created: String? = null
    var date_modified: String? = null

    var type: String? = null
    var status: String? = null
    var featured = false
    var catalog_visibility: String? = null
    var description: String? = null
    var short_description: String? = null
    var sku: String? = null

    var price: String? = null
    var regular_price: String? = null
    var sale_price: String? = null
    var on_sale = false
    var total_sales = 0

    var virtual = false
    var downloadable = false
    var download_limit = 0L
    var download_expiry = 0

    var external_url: String? = null
    val button_text: String? = null

    var tax_status: String? = null
    var tax_class: String? = null

    var manage_stock: String? = null
    var stock_quantity = 0
    var stock_status: String? = null

    var date_on_sale_from: String? = null
    var date_on_sale_to: String? = null
    var date_on_sale_from_gmt: String? = null
    var date_on_sale_to_gmt: String? = null

    var backorders: String? = null
    var backorders_allowed = false
    var backordered = false

    var sold_individually = false
    var weight: String? = null
    var dimensions: JsonElement? = null

    var shipping_required = false
    var shipping_taxable = false
    var shipping_class: String? = null
    var shipping_class_id = 0

    var reviews_allowed = true
    var average_rating: String? = null
    var rating_count = 0

    var parent_id = 0
    val menu_order = 0
    var purchase_note: String? = null

    var categories: JsonElement? = null
    var tags: JsonElement? = null
    var images: JsonElement? = null
    var attributes: JsonElement? = null
    var variations: JsonElement? = null
    var downloads: JsonElement? = null
    var related_ids: JsonElement? = null
    var cross_sell_ids: JsonElement? = null
    var upsell_ids: JsonElement? = null
    var grouped_products: JsonElement? = null

    fun asProductModel(): WCProductModel {
        val response = this
        return WCProductModel().apply {
            remoteProductId = response.id ?: 0
            name = response.name ?: ""
            slug = response.slug ?: ""
            permalink = response.permalink ?: ""

            dateCreated = response.date_created ?: ""
            dateModified = response.date_modified ?: ""

            dateOnSaleFrom = response.date_on_sale_from ?: ""
            dateOnSaleTo = response.date_on_sale_to ?: ""
            dateOnSaleFromGmt = response.date_on_sale_from_gmt ?: ""
            dateOnSaleToGmt = response.date_on_sale_to_gmt ?: ""

            type = response.type ?: ""
            status = response.status ?: ""
            featured = response.featured
            catalogVisibility = response.catalog_visibility ?: ""
            description = response.description ?: ""
            shortDescription = response.short_description ?: ""
            sku = response.sku ?: ""

            price = response.price ?: ""
            regularPrice = response.regular_price ?: ""
            salePrice = response.sale_price ?: ""
            onSale = response.on_sale
            totalSales = response.total_sales

            virtual = response.virtual
            downloadable = response.downloadable
            downloadLimit = response.download_limit
            downloadExpiry = response.download_expiry

            externalUrl = response.external_url ?: ""
            buttonText = response.button_text ?: ""

            taxStatus = response.tax_status ?: ""
            taxClass = response.tax_class ?: ""

            // variations may have "parent" here if inventory is enabled for the parent but not the variation
            manageStock = response.manage_stock?.let {
                it == "true" || it == "parent"
            } ?: false

            stockQuantity = response.stock_quantity
            stockStatus = response.stock_status ?: ""

            backorders = response.backorders ?: ""
            backordersAllowed = response.backorders_allowed
            backordered = response.backordered
            soldIndividually = response.sold_individually
            weight = response.weight ?: ""

            shippingRequired = response.shipping_required
            shippingTaxable = response.shipping_taxable
            shippingClass = response.shipping_class ?: ""
            shippingClassId = response.shipping_class_id

            reviewsAllowed = response.reviews_allowed
            averageRating = response.average_rating ?: ""
            ratingCount = response.rating_count

            parentId = response.parent_id
            menuOrder = response.menu_order
            purchaseNote = response.purchase_note ?: ""

            categories = response.categories?.toString() ?: ""
            tags = response.tags?.toString() ?: ""
            images = response.images?.toString() ?: ""
            attributes = response.attributes?.toString() ?: ""
            variations = response.variations?.toString() ?: ""
            downloads = response.downloads?.toString() ?: ""
            relatedIds = response.related_ids?.toString() ?: ""
            crossSellIds = response.cross_sell_ids?.toString() ?: ""
            upsellIds = response.upsell_ids?.toString() ?: ""
            groupedProductIds = response.grouped_products?.toString() ?: ""

            response.dimensions?.asJsonObject?.let { json ->
                length = json.getString("length") ?: ""
                width = json.getString("width") ?: ""
                height = json.getString("height") ?: ""
            }
        }
    }
}
