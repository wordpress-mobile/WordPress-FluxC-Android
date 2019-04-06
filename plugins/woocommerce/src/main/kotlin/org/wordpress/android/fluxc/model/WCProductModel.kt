package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
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
    @Column var remoteVariationId = 0L
    @Column var name = ""
    @Column var slug = ""
    @Column var permalink = ""
    @Column var imageUrl = ""

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
    @Column(name = "IS_VIRTUAL") var virtual = false

    @Column var downloadable = false
    @Column var downloadLimit = 0
    @Column var downloadExpiry = 0
    @Column var soldIndividually = false
    @Column var purchasable = false
    @Column var externalUrl = ""

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
    @Column var attributes = "" // array of attributes
    @Column var variations = "" // array of variation IDs
    @Column var downloads = "" // array of downloadable files
    @Column var relatedIds = "" // array of related product IDs
    @Column var crossSellIds = "" // array of cross-sell product IDs
    @Column var upsellIds = "" // array of up-sell product IDs

    @Column var weight = ""
    @Column var length = ""
    @Column var width = ""
    @Column var height = ""

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }

    fun getDownloadableFiles(): List<String> {
        val fileList = ArrayList<String>()
        try {
            Gson().fromJson<JsonElement>(downloads, JsonElement::class.java).asJsonArray.forEach { jsonElement ->
                jsonElement.asJsonObject.getString("file")?.let {
                    fileList.add(it)
                }
            }
        } catch (e: JsonParseException) {
            AppLog.e(T.API, e)
        }
        return fileList
    }

    /**
     * Updates this product model to use the values from the passed variation model
     */
    fun updateFromVariation(variation: WCProductVariationModel) {
        remoteVariationId = variation.remoteVariationId
        dateCreated = variation.dateCreated
        dateModified = variation.dateModified
        description = variation.description
        permalink = variation.permalink
        sku = variation.sku
        status = variation.status

        price = variation.price
        regularPrice = variation.regularPrice
        salePrice = variation.salePrice
        onSale = variation.onSale

        purchasable = variation.purchasable
        virtual = variation.virtual

        downloadable = variation.downloadable
        downloadLimit = variation.downloadLimit
        downloadExpiry = variation.downloadExpiry

        taxClass = variation.taxClass
        taxStatus = variation.taxStatus

        backorders = variation.backorders
        backordered = variation.backordered
        backordersAllowed = variation.backordersAllowed

        manageStock = variation.manageStock
        stockQuantity = variation.stockQuantity
        stockStatus = variation.stockStatus

        shippingClass = variation.shippingClass
        shippingClassId = variation.shippingClassId

        weight = variation.weight
        length = variation.length
        width = variation.width
        height = variation.height

        imageUrl = variation.imageUrl
        attributes = variation.attributes
    }
}
