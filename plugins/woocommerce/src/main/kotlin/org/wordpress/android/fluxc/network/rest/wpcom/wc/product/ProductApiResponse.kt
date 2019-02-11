package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.JsonElement
import org.wordpress.android.fluxc.network.Response

@Suppress("PropertyName")
class ProductApiResponse : Response {
    val id: Long? = null
    var localSiteId = 0
    var name = ""
    var slug = ""
    var permalink = ""

    var date_created = ""
    var date_created_gmt = ""
    var date_modified = ""
    var date_modified_gmt = ""

    var type = ""                   // simple, grouped, external, variable
    var status = ""
    var featured = false
    var catalog_visibility = ""     // visible, catalog, search, hidden
    var description = ""
    var short_description = ""
    var sku = ""

    var price = ""
    var price_html = ""
    var regular_price = ""
    var sale_price = ""
    var date_on_sale_from = ""
    var date_on_sale_from_gmt = ""
    var date_on_sale_to = ""
    var date_on_sale_to_gmt = ""
    var on_sale = false
    var total_sales = 0

    var virtual = false
    var downloadable = false
    var downloads = ""              // array of downloadable files
    var download_limit = -1
    var download_expiry = -1

    var external_url = ""
    var button_text = ""

    var tax_status = ""             // taxable, shipping, none
    var tax_class = ""

    var manage_stock = false
    var stock_quantity = 0
    var stock_status = ""           // instock, outofstock, onbackorder

    var backorders = ""             // no, notify, yes
    var backorders_allowed = false
    var backordered = false

    var sold_individually = false
    var weight = ""
    var dimensions = ""             // TODO: docs list this as an object

    var shipping_required = false
    var shipping_taxable = false
    var shipping_class = ""
    var shipping_class_id = 0

    var reviews_allowed = true
    var average_rating = ""
    var rating_count = 0

    var related_ids = ""            // array of related product IDs
    var upsell_ids = ""             // array of up-sell product IDs
    var cross_sell_ids = ""         // array of cross-sell product IDs

    var parent_id = 0
    var purchase_note = ""
    var menu_order = 0

    var categories = ""             // array of categories
    var tags = ""                   // array of tags
    var images = ""                 // array of images
    var attributes = ""             // array of attributes
    var default_attributes = ""     // array of default attributes
    var variations = ""             // array of variation IDs
    var grouped_products = ""       // array of grouped product IDs
    var meta_data = ""              // array of metadata
}
