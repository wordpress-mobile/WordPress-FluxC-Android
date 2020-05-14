package org.wordpress.android.fluxc.model.shippinglabels

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
class WCShippingLabelModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column var localOrderId = 0L // The local db unique identifier for the parent order object
    @Column var remoteShippingLabelId = 0L // The unique identifier for this note on the server
    @Column var trackingNumber = ""
    @Column var carrierId = ""
    @Column var serviceName = ""
    @Column var status = ""
    @Column var packageName = ""
    @Column var rate = 0F
    @Column var refundableAmount = 0F
    @Column var currency = ""
    @Column var paperSize = ""
    @Column var productNames = "" // list of product names the shipping label was purchased for

    @Column var formData = "" // map containing package and product details related to that shipping label
    @Column var storeOptions = "" // map containing store settings such as currency and dimensions

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }

    companion object {
        private val gson by lazy { Gson() }
    }

    /**
     * Returns the destination details wrapped in a [ShippingLabelAddress].
     */
    fun getDestinationAddress() = getFormData()?.destination

    /**
     * Returns the shipping details wrapped in a [ShippingLabelAddress].
     */
    fun getOriginAddress() = getFormData()?.origin

    /**
     * Returns the product details for the order wrapped in a list of [ProductItem]
     */
    fun getProductItems() = getFormData()?.selectedPackage?.defaultBox?.productItems ?: emptyList()

    /**
     * Returns the store details such as currency, country and dimensions wrapped in [StoreOptions]
     */
    fun getStoreOptions(): StoreOptions? {
        val responseType = object : TypeToken<StoreOptions>() {}.type
        return gson.fromJson(storeOptions, responseType) as? StoreOptions
    }

    /**
     * Returns default data related to the order such as the origin address,
     * destination address and product items associated with the order.
     */
    private fun getFormData(): FormData? {
        val responseType = object : TypeToken<FormData>() {}.type
        return gson.fromJson(formData, responseType) as? FormData
    }

    /**
     * Returns the list of products the shipping labels were purchased for
     */
    fun getProductNames(): List<String> {
        val responseType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(productNames, responseType) as? List<String> ?: emptyList()
    }

    class StoreOptions {
        @SerializedName("currency_symbol") val currencySymbol: String? = null
        @SerializedName("dimension_unit") val dimensionUnit: String? = null
        @SerializedName("weight_unit") val weightUnit: String? = null
        @SerializedName("origin_country") val originCountry: String? = null
    }

    /**
     * Model class corresponding to the [formData] map from the API response.
     * The [formData] contains the [origin] and [destination] address and the
     * product details associated with the order.
     * (nested under [selectedPackage] -> [DefaultBox] -> List of [ProductItem]).
     */
    class FormData {
        val origin: ShippingLabelAddress? = null
        val destination: ShippingLabelAddress? = null
        @SerializedName("selected_packages") val selectedPackage: SelectedPackage? = null
    }

    class ShippingLabelAddress {
        val company: String? = null
        val name: String? = null
        val phone: String? = null
        val country: String? = null
        val state: String? = null
        val address: String? = null
        val address2: String? = null
        val city: String? = null
        val postcode: String? = null
    }

    class SelectedPackage {
        @SerializedName("default_box") val defaultBox: DefaultBox? = null
    }

    class DefaultBox {
        @SerializedName("items") val productItems: List<ProductItem>? = null
    }

    class ProductItem {
        val height: Int? = null
        val length: Int? = null
        val quantity: Int? = null
        val width: Int? = null
        val name: String? = null
        val url: String? = null
        val value: Int? = null
        @SerializedName("product_id") val productId: Long? = null
    }
}
