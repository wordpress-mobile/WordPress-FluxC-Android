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
 * Note that products have more properties than we support below
 */
@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCProductModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column var remoteProductId = 0L // The unique identifier for this product on the server
    @Column var name = ""
    @Column var slug = ""
    @Column var permalink = ""

    @Column var dateCreated = ""
    @Column var dateModified = ""

    @Column var type = "" // simple, grouped, external, variable
    @Column var status = ""
    @Column var featured = false
    @Column var catalogVisibility = "" // visible, catalog, search, hidden
    @Column var description = ""
    @Column var shortDescription = ""
    @Column var sku = ""

    @Column var price = ""
    @Column var regularPrice = ""
    @Column var salePrice = ""
    @Column var onSale = false
    @Column var totalSales = 0

    @Column var virtual = false
    @Column var downloadable = false
    @Column var soldIndividually = false

    @Column var taxStatus = "" // taxable, shipping, none
    @Column var taxClass = ""

    @Column var manageStock = false
    @Column var stockQuantity = 0
    @Column var stockStatus = "" // instock, outofstock, onbackorder

    @Column var backorders = "" // no, notify, yes
    @Column var backordersAllowed = false
    @Column var backordered = false

    @Column var shippingRequired = false
    @Column var shippingTaxable = false
    @Column var shippingClass = ""
    @Column var shippingClassId = 0

    @Column var reviewsAllowed = true
    @Column var averageRating = ""
    @Column var ratingCount = 0

    @Column var parentId = 0
    @Column var purchaseNote = ""

    @Column var categories = "" // array of categories
    @Column var tags = "" // array of tags
    @Column var images = "" // array of images
    @Column var attributes = "" // array of attributes
    @Column var variations = "" // array of variation IDs

    @Column var weight = ""
    @Column var length = ""
    @Column var width = ""
    @Column var height = ""

    class ProductTriplet(val id: Long, val name: String, val slug: String)

    class ProductImage(val id: Long, val name: String, val src: String, val alt: String)

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }

    /**
     * Parses the images json array into a list of product images
     */
    fun getImages(): ArrayList<ProductImage> {
        val imageList = ArrayList<ProductImage>()
        try {
            Gson().fromJson<JsonElement>(images, JsonElement::class.java).asJsonArray.forEach { jsonElement ->
                with(jsonElement.asJsonObject) {
                    imageList.add(
                            ProductImage(
                                    id = this.getLong("id"),
                                    name = this.getString("name") ?: "",
                                    src = this.getString("src") ?: "",
                                    alt = this.getString("alt") ?: ""
                            )
                    )
                }
            }
        } catch (e: JsonParseException) {
            AppLog.e(T.API, e)
        }
        return imageList
    }

    /**
     * Extract the first image from the json array of images
     */
    fun getFirstImage(): String? {
        try {
            Gson().fromJson<JsonElement>(images, JsonElement::class.java).asJsonArray.first() { jsonElement ->
                return (jsonElement.asJsonObject).getString("src")
            }
        } catch (e: JsonParseException) {
            AppLog.e(T.API, e)
        }
        return null
    }

    fun getCategories() = getTriplets(categories)

    fun getTags() = getTriplets(tags)

    private fun getTriplets(jsonStr: String): ArrayList<ProductTriplet> {
        val triplets = ArrayList<ProductTriplet>()
        try {
            Gson().fromJson<JsonElement>(jsonStr, JsonElement::class.java).asJsonArray.forEach { jsonElement ->
                with(jsonElement.asJsonObject) {
                    triplets.add(
                            ProductTriplet(
                                    id = this.getLong("id"),
                                    name = this.getString("name") ?: "",
                                    slug = this.getString("slug") ?: ""
                            )
                    )
                }
            }
        } catch (e: JsonParseException) {
            AppLog.e(T.API, e)
        }
        return triplets
    }
}
