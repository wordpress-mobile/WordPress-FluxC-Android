package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
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
 * Product variations - see http://woocommerce.github.io/woocommerce-rest-api-docs/#product-variations
 * As with WCProductModel, the backend returns more properties than are supported below
 */
@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCProductVariationModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column var remoteProductId = 0L
    @Column var remoteVariationId = 0L

    @Column var dateCreated = ""
    @Column var dateModified = ""

    @Column var description = ""
    @Column var permalink = ""
    @Column var sku = ""
    @Column var status = ""

    @Column var price = ""
    @Column var regularPrice = ""
    @Column var salePrice = ""

    @Column var dateOnSaleFrom = ""
    @Column var dateOnSaleTo = ""
    @Column var dateOnSaleFromGmt = ""
    @Column var dateOnSaleToGmt = ""

    @Column var onSale = false
    @Column var purchasable = false
    @Column var virtual = false
    @Column var downloadable = false

    @Column var manageStock = false
    @Column var stockQuantity = 0
    @Column var stockStatus = ""

    @Column var image = ""

    @Column var weight = ""
    @Column var length = ""
    @Column var width = ""
    @Column var height = ""

    @Column var menuOrder = 0

    @Column var attributes = ""

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }

    class ProductVariantOption {
        val id: Long? = null
        val name: String? = null
        val option: String? = null
    }

    /**
     * Parses the images json array into a list of product images
     */
    fun getImage(): WCProductImageModel? {
        if (image.isNotBlank()) {
            try {
                with(Gson().fromJson(image, JsonElement::class.java).asJsonObject) {
                    WCProductImageModel(this.getLong("id")).also {
                        it.name = this.getString("name") ?: ""
                        it.src = this.getString("src") ?: ""
                        it.alt = this.getString("alt") ?: ""
                        return it
                    }
                }
            } catch (e: JsonParseException) {
                AppLog.e(T.API, e)
                return null
            }
        }
        return null
    }

    /**
     * Deserializes the JSON contained in [attributes] into a list of [ProductVariantOption] objects.
     */
    fun getProductVariantOptions(): List<ProductVariantOption> {
        val responseType = object : TypeToken<List<ProductVariantOption>>() {}.type
        return Gson().fromJson(attributes, responseType) as? List<ProductVariantOption> ?: emptyList()
    }
}
