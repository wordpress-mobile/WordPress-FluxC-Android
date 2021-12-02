package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.JsonElement
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.utils.DateUtils

@Suppress("PropertyName")
class OrderDto : Response {
    data class Billing(
        val first_name: String? = null,
        val last_name: String? = null,
        val company: String? = null,
        val address_1: String? = null,
        val address_2: String? = null,
        val city: String? = null,
        val state: String? = null,
        val postcode: String? = null,
        val country: String? = null,
        val email: String? = null,
        val phone: String? = null
    )

    data class Shipping(
        val first_name: String? = null,
        val last_name: String? = null,
        val company: String? = null,
        val address_1: String? = null,
        val address_2: String? = null,
        val city: String? = null,
        val state: String? = null,
        val postcode: String? = null,
        val country: String? = null,
        val phone: String? = null
    )

    class CouponLine {
        val code: String? = null
    }

    class Refund {
        val total: String? = null
    }

    val billing: Billing? = null
    val coupon_lines: List<CouponLine>? = null
    val currency: String? = null
    val order_key: String? = null
    val customer_note: String? = null
    val date_created_gmt: String? = null
    val date_modified_gmt: String? = null
    val date_paid_gmt: String? = null
    val discount_total: String? = null
    // Same as shipping_lines, it's a list of objects
    val fee_lines: JsonElement? = null
    val id: Long? = null
    // This is actually a list of objects. We're storing this as JSON initially, and it will be deserialized on demand.
    // See WCOrderModel.LineItem
    val line_items: JsonElement? = null
    val number: String? = null
    val payment_method: String? = null
    val payment_method_title: String? = null
    val prices_include_tax: Boolean = false
    val refunds: List<Refund>? = null
    val shipping: Shipping? = null
    // This is actually a list of objects. We're storing this as JSON initially, and it will be deserialized on demand.
    // See WCOrderModel.ShippingLines
    val shipping_lines: JsonElement? = null
    val shipping_total: String? = null
    val status: String? = null
    val total: String? = null
    val total_tax: String? = null
    val meta_data: JsonElement? = null
}

fun OrderDto.toDomainModel(localSiteId: Int): WCOrderModel {
    fun convertDateToUTCString(date: String?): String =
            date?.let { DateUtils.formatGmtAsUtcDateString(it) } ?: "" // Store the date in UTC format

    return WCOrderModel().apply {
        this.localSiteId = localSiteId
        remoteOrderId = this@toDomainModel.id ?: 0
        number = this@toDomainModel.number ?: remoteOrderId.toString()
        status = this@toDomainModel.status ?: ""
        currency = this@toDomainModel.currency ?: ""
        orderKey = this@toDomainModel.order_key ?: ""
        dateCreated = convertDateToUTCString(this@toDomainModel.date_created_gmt)
        dateModified = convertDateToUTCString(this@toDomainModel.date_modified_gmt)
        total = this@toDomainModel.total ?: ""
        totalTax = this@toDomainModel.total_tax ?: ""
        shippingTotal = this@toDomainModel.shipping_total ?: ""
        paymentMethod = this@toDomainModel.payment_method ?: ""
        paymentMethodTitle = this@toDomainModel.payment_method_title ?: ""
        datePaid = this@toDomainModel.date_paid_gmt?.let { "${it}Z" } ?: ""
        pricesIncludeTax = this@toDomainModel.prices_include_tax

        customerNote = this@toDomainModel.customer_note ?: ""

        discountTotal = this@toDomainModel.discount_total ?: ""
        this@toDomainModel.coupon_lines?.let { couponLines ->
            // Extract the discount codes from the coupon_lines list and store them as a comma-delimited String
            discountCodes = couponLines
                    .filter { !it.code.isNullOrEmpty() }
                    .joinToString { it.code!! }
        }

        this@toDomainModel.refunds?.let { refunds ->
            // Extract the individual refund totals from the refunds list and store their sum as a Double
            refundTotal = refunds.sumByDouble { it.total?.toDoubleOrNull() ?: 0.0 }
        }

        billingFirstName = this@toDomainModel.billing?.first_name ?: ""
        billingLastName = this@toDomainModel.billing?.last_name ?: ""
        billingCompany = this@toDomainModel.billing?.company ?: ""
        billingAddress1 = this@toDomainModel.billing?.address_1 ?: ""
        billingAddress2 = this@toDomainModel.billing?.address_2 ?: ""
        billingCity = this@toDomainModel.billing?.city ?: ""
        billingState = this@toDomainModel.billing?.state ?: ""
        billingPostcode = this@toDomainModel.billing?.postcode ?: ""
        billingCountry = this@toDomainModel.billing?.country ?: ""
        billingEmail = this@toDomainModel.billing?.email ?: ""
        billingPhone = this@toDomainModel.billing?.phone ?: ""

        shippingFirstName = this@toDomainModel.shipping?.first_name ?: ""
        shippingLastName = this@toDomainModel.shipping?.last_name ?: ""
        shippingCompany = this@toDomainModel.shipping?.company ?: ""
        shippingAddress1 = this@toDomainModel.shipping?.address_1 ?: ""
        shippingAddress2 = this@toDomainModel.shipping?.address_2 ?: ""
        shippingCity = this@toDomainModel.shipping?.city ?: ""
        shippingState = this@toDomainModel.shipping?.state ?: ""
        shippingPostcode = this@toDomainModel.shipping?.postcode ?: ""
        shippingCountry = this@toDomainModel.shipping?.country ?: ""
        shippingPhone = this@toDomainModel.shipping?.phone.orEmpty()

        lineItems = this@toDomainModel.line_items.toString()
        shippingLines = this@toDomainModel.shipping_lines.toString()
        feeLines = this@toDomainModel.fee_lines.toString()
        metaData = this@toDomainModel.meta_data.toString()
    }
}
