package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.model.order.OrderAddress.AddressType
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.model.order.OrderProductAttributeListDeserializer
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import java.util.Locale

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCOrderModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column var remoteOrderId = 0L // The unique identifier for this order on the server
    @Column var number = "" // The order number to display to the user
    @Column var status = ""
    @Column var currency = ""
    @Column var dateCreated = "" // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @Column var dateModified = "" // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @Column var total = "" // Complete total, including taxes

    @Column var totalTax = "" // The total amount of tax (from products, shipping, discounts, etc.)
    @Column var shippingTotal = "" // The total shipping cost (excluding tax)
    @Column var paymentMethod = "" // Payment method code, e.g. 'cod', 'stripe'
    @Column var paymentMethodTitle = "" // Displayable payment method, e.g. 'Cash on delivery', 'Credit Card (Stripe)'
    @Column var datePaid = ""
    @Column var pricesIncludeTax = false

    @Column var customerNote = "" // Note left by the customer during order submission

    @Column var discountTotal = ""
    @Column var discountCodes = ""

    @Column var refundTotal = 0.0 // The total refund value for this order (usually a negative number)

    @Column var billingFirstName = ""
    @Column var billingLastName = ""
    @Column var billingCompany = ""
    @Column var billingAddress1 = ""
    @Column var billingAddress2 = ""
    @Column var billingCity = ""
    @Column var billingState = ""
    @Column var billingPostcode = ""
    @Column var billingCountry = ""
    @Column var billingEmail = ""
    @Column var billingPhone = ""

    @Column var shippingFirstName = ""
    @Column var shippingLastName = ""
    @Column var shippingCompany = ""
    @Column var shippingAddress1 = ""
    @Column var shippingAddress2 = ""
    @Column var shippingCity = ""
    @Column var shippingState = ""
    @Column var shippingPostcode = ""
    @Column var shippingCountry = ""

    @Column var lineItems = ""

    @Column var shippingLines = ""

    companion object {
        private val gson by lazy { Gson() }
    }

    class ShippingLine {
        @SerializedName("method_title")
        val methodTitle: String? = null
    }

    class LineItem {
        val id: Long? = null
        val name: String? = null
        @SerializedName("parent_name")
        val parentName: String? = null
        @SerializedName("product_id")
        val productId: Long? = null
        @SerializedName("variation_id")
        val variationId: Long? = null
        val quantity: Float? = null
        val subtotal: String? = null
        val total: String? = null // Price x quantity
        @SerializedName("total_tax")
        val totalTax: String? = null
        val sku: String? = null
        val price: String? = null // The per-item price

        @SerializedName("meta_data")
        private val attributes: JsonArray? = null

        class Attribute(val key: String?, val value: String?)

        fun getAttributeList(): List<Attribute> {
            val responseType = object : TypeToken<List<Attribute>>() {}.type
            val newGson = gson.newBuilder()
                    .registerTypeAdapter(responseType, OrderProductAttributeListDeserializer()).create()
            return newGson.fromJson(attributes, responseType)
        }

        /**
         * @return a comma-separated list of attribute values for display
         */
        fun getAttributesAsString(): String {
            return getAttributeList()
                    .filter {
                        // Don't include null, empty, or the "_reduced_stock" key
                        // skipping "_reduced_stock" is a temporary workaround until "type" is added to the response.
                        it.value != null && it.value.isNotEmpty()
                                && it.key != null && it.key.isNotEmpty() && it.key.first().toString() != "_"
                    }.joinToString { it.value?.capitalize(Locale.getDefault()) ?: "" }
        }
    }

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }

    /**
     * Returns an [OrderIdentifier], representing a unique identifier for this [WCOrderModel].
     */
    fun getIdentifier() = OrderIdentifier(this)

    /**
     * Returns true if there are shipping details defined for this order,
     * which are different from the billing details.
     *
     * If no separate shipping details are defined, the billing details should be used instead,
     * as the shippingX properties will be empty.
     */
    fun hasSeparateShippingDetails() = shippingCountry.isNotEmpty()

    /**
     * Returns the billing details wrapped in a [OrderAddress].
     */
    fun getBillingAddress() = OrderAddress(this, AddressType.BILLING)

    /**
     * Returns the shipping details wrapped in a [OrderAddress].
     */
    fun getShippingAddress() = OrderAddress(this, AddressType.SHIPPING)

    /**
     * Deserializes the JSON contained in [lineItems] into a list of [LineItem] objects.
     */
    fun getLineItemList(): List<LineItem> {
        val responseType = object : TypeToken<List<LineItem>>() {}.type
        return gson.fromJson(lineItems, responseType) as? List<LineItem> ?: emptyList()
    }

    /**
     * Returns the order subtotal (the sum of the subtotals of each line item in the order).
     */
    fun getOrderSubtotal(): Double {
        return getLineItemList().sumByDouble { it.subtotal?.toDoubleOrNull() ?: 0.0 }
    }

    /**
     * Deserializes the JSON contained in [shippingLines] into a list of [ShippingLine] objects.
     */
    fun getShippingLineList(): List<ShippingLine> {
        val responseType = object : TypeToken<List<ShippingLine>>() {}.type
        return gson.fromJson(shippingLines, responseType) as? List<ShippingLine> ?: emptyList()
    }

    fun isMultiShippingLinesAvailable() = getShippingLineList().size > 1
}
