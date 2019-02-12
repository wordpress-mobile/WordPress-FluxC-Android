package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.network.utils.getLong
import org.wordpress.android.fluxc.network.utils.getString
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

/**
 * Single Woo product - see http://woocommerce.github.io/woocommerce-rest-api-docs/#product-properties
 * TODO: remove underscores in property names
 */
@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCProductModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column var remoteProductId = 0L // The unique identifier for this product on the server
    @Column var name = ""
    @Column var slug = ""
    @Column var permalink = ""

    @Column var date_created = ""
    @Column var date_created_gmt = ""
    @Column var date_modified = ""
    @Column var date_modified_gmt = ""

    @Column var type = "" // simple, grouped, external, variable
    @Column var status = ""
    @Column var featured = false
    @Column var catalog_visibility = "" // visible, catalog, search, hidden
    @Column var description = ""
    @Column var short_description = ""
    @Column var sku = ""

    @Column var price = ""
    @Column var regular_price = ""
    @Column var sale_price = ""
    @Column var date_on_sale_from = ""
    @Column var date_on_sale_from_gmt = ""
    @Column var date_on_sale_to = ""
    @Column var date_on_sale_to_gmt = ""
    @Column var on_sale = false
    @Column var total_sales = 0

    @Column var virtual = false
    @Column var downloadable = false
    @Column var downloads = "" // array of downloadable files
    @Column var download_limit = -1
    @Column var download_expiry = -1

    @Column var external_url = ""
    @Column var button_text = ""

    @Column var tax_status = "" // taxable, shipping, none
    @Column var tax_class = ""

    @Column var manage_stock = false
    @Column var stock_quantity = 0
    @Column var stock_status = "" // instock, outofstock, onbackorder

    @Column var backorders = "" // no, notify, yes
    @Column var backorders_allowed = false
    @Column var backordered = false

    @Column var sold_individually = false
    @Column var weight = ""

    @Column var shipping_required = false
    @Column var shipping_taxable = false
    @Column var shipping_class = ""
    @Column var shipping_class_id = 0

    @Column var reviews_allowed = true
    @Column var average_rating = ""
    @Column var rating_count = 0

    @Column var related_ids = "" // array of related product IDs
    @Column var upsell_ids = ""  // array of up-sell product IDs
    @Column var cross_sell_ids = "" // array of cross-sell product IDs

    @Column var parent_id = 0
    @Column var purchase_note = ""
    @Column var menu_order = 0

    @Column var categories = "" // array of categories
    @Column var tags = "" // array of tags
    @Column var images = "" // array of images
    @Column var attributes = "" // array of attributes
    @Column var default_attributes = "" // array of default attributes
    @Column var variations = "" // array of variation IDs
    @Column var grouped_products = "" // array of grouped product IDs
    @Column var meta_data = "" // array of metadata

    // Dimensions
    @Column var length = ""
    @Column var width = ""
    @Column var height = ""

    class Category {
        var id = 0L
        var name = ""
        var slug = ""
    }

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }

    /**
     * Return the json categories as a list of category objects
     */
    fun getCategories(): List<Category> {
        val cats = ArrayList<Category>()
        val gson = Gson()
        try {
            val jsonCats = gson.fromJson<JsonElement>(categories, JsonElement::class.java)
            jsonCats.asJsonArray.forEach { jsonElement ->
                with(jsonElement.asJsonObject) {
                    val cat = Category()
                    if (this.has("id")) cat.id = this.getLong("id")
                    if (this.has("name")) cat.name = this.getString("name") ?: ""
                    if (this.has("slug")) cat.slug = this.getString("slug") ?: ""
                    cats.add(cat)
                }
            }
        } catch (e: JsonParseException) {
            AppLog.e(T.API, e)
        }
        return cats
    }
}
