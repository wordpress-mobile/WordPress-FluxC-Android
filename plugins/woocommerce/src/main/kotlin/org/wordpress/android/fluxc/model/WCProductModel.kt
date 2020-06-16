package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.network.utils.getBoolean
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

    @Column var dateOnSaleFrom = ""
    @Column var dateOnSaleTo = ""
    @Column var dateOnSaleFromGmt = ""
    @Column var dateOnSaleToGmt = ""

    @Column var virtual = false
    @Column var downloadable = false
    @Column var downloadLimit = 0
    @Column var downloadExpiry = 0
    @Column var soldIndividually = false

    @Column var externalUrl = ""
    @Column var buttonText = ""

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
    @Column var menuOrder = 0

    @Column var categories = "" // array of categories
    @Column var tags = "" // array of tags
    @Column var images = "" // array of images
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

    class ProductTriplet(val id: Long, val name: String, val slug: String) {
        fun toJson(): JsonObject {
            return JsonObject().also { json ->
                json.addProperty("id", id)
                json.addProperty("name", name)
                json.addProperty("slug", slug)
            }
        }
    }

    class ProductAttribute(val id: Long, val name: String, val visible: Boolean, val options: List<String>) {
        fun getCommaSeparatedOptions(): String {
            if (options.isEmpty()) return ""
            var commaSeparatedOptions = ""
            options.forEach { option ->
                if (commaSeparatedOptions.isEmpty()) {
                    commaSeparatedOptions = option
                } else {
                    commaSeparatedOptions += ", $option"
                }
            }
            return commaSeparatedOptions
        }
    }

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }

    /**
     * Parses the images json array into a list of product images
     */
    fun getImages(): ArrayList<WCProductImageModel> {
        val imageList = ArrayList<WCProductImageModel>()
        if (images.isNotEmpty()) {
            try {
                Gson().fromJson(images, JsonElement::class.java).asJsonArray.forEach { jsonElement ->
                    with(jsonElement.asJsonObject) {
                        WCProductImageModel(this.getLong("id")).also {
                            it.name = this.getString("name") ?: ""
                            it.src = this.getString("src") ?: ""
                            it.alt = this.getString("alt") ?: ""
                            imageList.add(it)
                        }
                    }
                }
            } catch (e: JsonParseException) {
                AppLog.e(T.API, e)
            } catch (e: IllegalStateException) {
                AppLog.e(T.API, e)
            }
        }
        return imageList
    }

    /**
     * Extract the first image url from the json array of images
     */
    fun getFirstImageUrl(): String? {
        try {
            Gson().fromJson(images, JsonElement::class.java).asJsonArray.firstOrNull { jsonElement ->
                return (jsonElement.asJsonObject).getString("src")
            }
        } catch (e: JsonParseException) {
            AppLog.e(T.API, e)
        }
        return null
    }

    /**
     * Extract all image urls from the json array of images
     */
    fun getImageUrls(): List<String> {
        val imageUrls = ArrayList<String>()
        getImages().forEach {
            imageUrls.add(it.src)
        }
        return imageUrls
    }

    fun getAttributes(): List<ProductAttribute> {
        fun getAttributeOptions(jsonArray: JsonArray?): List<String> {
            val options = ArrayList<String>()
            try {
                jsonArray?.forEach {
                    options.add(it.asString)
                }
            } catch (e: ClassCastException) {
                AppLog.e(T.API, e)
            } catch (e: IllegalStateException) {
                AppLog.e(T.API, e)
            }
            return options
        }

        val attrList = ArrayList<ProductAttribute>()
        try {
            Gson().fromJson<JsonElement>(attributes, JsonElement::class.java).asJsonArray.forEach { jsonElement ->
                with(jsonElement.asJsonObject) {
                    attrList.add(
                            ProductAttribute(
                                    id = this.getLong("id"),
                                    name = this.getString("name") ?: "",
                                    visible = this.getBoolean("visible", true),
                                    options = getAttributeOptions(this.getAsJsonArray("options"))
                            )
                    )
                }
            }
        } catch (e: JsonParseException) {
            AppLog.e(T.API, e)
        }
        return attrList
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

    fun getNumVariations(): Int {
        try {
            return Gson().fromJson<JsonElement>(variations, JsonElement::class.java).asJsonArray.size()
        } catch (e: JsonParseException) {
            AppLog.e(T.API, e)
            return 0
        }
    }

    fun getCategories() = getTriplets(categories)

    fun getCommaSeparatedCategoryNames() = getCommaSeparatedTripletNames(getCategories())

    fun getTags() = getTriplets(tags)

    fun getCommaSeparatedTagNames() = getCommaSeparatedTripletNames(getTags())

    private fun getCommaSeparatedTripletNames(triplets: List<ProductTriplet>): String {
        if (triplets.isEmpty()) return ""
        var commaSeparatedNames = ""
        triplets.forEach {
            if (commaSeparatedNames.isEmpty()) {
                commaSeparatedNames = it.name
            } else {
                commaSeparatedNames += ", ${it.name}"
            }
        }
        return commaSeparatedNames
    }

    private fun getTriplets(jsonStr: String): ArrayList<ProductTriplet> {
        val triplets = ArrayList<ProductTriplet>()
        try {
            if (jsonStr.isNotEmpty()) {
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
            }
        } catch (e: JsonParseException) {
            AppLog.e(T.API, e)
        }
        return triplets
    }

    /**
     * Compares this product's images with the passed product's images, returns true only if both
     * lists contain the same images in the same order
     */
    fun hasSameImages(updatedProduct: WCProductModel): Boolean {
        val updatedImages = updatedProduct.getImages()
        val thisImages = getImages()
        if (thisImages.size != updatedImages.size) {
            return false
        }
        for (i in thisImages.indices) {
            if (thisImages[i].id != updatedImages[i].id) {
                return false
            }
        }
        return true
    }

    /**
     * Compares this product's categories with the passed product's categories, returns true only if both
     * lists contain the same categories in the same order
     */
    fun hasSameCategories(updatedProduct: WCProductModel): Boolean {
        val updatedCategories = updatedProduct.getCategories()
        val storedCategories = getCategories()
        if (storedCategories.size != updatedCategories.size) {
            return false
        }
        for (i in storedCategories.indices) {
            if (storedCategories[i].id != updatedCategories[i].id) {
                return false
            }
        }
        return true
    }
}
