package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCOrderModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column var remoteOrderId = 0L // The unique identifier for this order on the server
    @Column var number = "" // The order number to display to the user
    @Column var status = ""
    @Column var currency = ""
    @Column var orderKey = ""
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
    @Column var shippingPhone = ""

    @Column var lineItems = ""

    @Column var shippingLines = ""

    @Column var feeLines = ""

    @Column var metaData = ""

    companion object {
        private val gson by lazy { Gson() }
    }

    class ShippingLine {
        val id: Long? = null
        val total: String? = null
        @SerializedName("total_tax")
        val totalTax: String? = null
        @SerializedName("method_id")
        val methodId: String? = null
        @SerializedName("method_title")
        val methodTitle: String? = null
    }

    /**
     * Represents a fee line
     * We are reading only the name and the total, as the tax is already included in the order totalTax
     */
    class FeeLine {
        @SerializedName("name")
        val name: String? = null

        @SerializedName("total")
        val total: String? = null
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
        val metaData: List<WCMetaData>? = null

        class Attribute(val key: String?, val value: String?)

        fun getAttributeList(): List<Attribute> {
            return metaData?.filter {
                it.displayKey is String && it.displayValue is String
            }?.map {
                Attribute(it.displayKey, it.displayValue as String)
            } ?: emptyList()
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
    fun getBillingAddress() = OrderAddress.Billing(this)

    /**
     * Returns the shipping details wrapped in a [OrderAddress].
     */
    fun getShippingAddress() = OrderAddress.Shipping(this)

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

    /**
     * Deserializes the JSON contained in [feeLines] into a list of [FeeLine] objects.
     */
    fun getFeeLineList(): List<FeeLine> {
        val responseType = object : TypeToken<List<FeeLine>>() {}.type
        return gson.fromJson(feeLines, responseType) as? List<FeeLine> ?: emptyList()
    }

    /**
     * Deserializes the JSON contained in [metaData] into a list of [WCMetaData] objects.
     */
    fun getMetaDataList(): List<WCMetaData> {
        val responseType = object : TypeToken<List<WCMetaData>>() {}.type
        return gson.fromJson(metaData, responseType) as? List<WCMetaData> ?: emptyList()
    }

    fun isMultiShippingLinesAvailable() = getShippingLineList().size > 1
}
