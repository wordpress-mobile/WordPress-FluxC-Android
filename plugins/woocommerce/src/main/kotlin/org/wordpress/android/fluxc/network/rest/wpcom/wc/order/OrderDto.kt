package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.JsonElement
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.utils.DateUtils
import java.math.BigDecimal

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
    // See OrderEntity.LineItem
    val line_items: JsonElement? = null
    val number: String? = null
    val payment_method: String? = null
    val payment_method_title: String? = null
    val prices_include_tax: Boolean = false
    val refunds: List<Refund>? = null
    val shipping: Shipping? = null
    // This is actually a list of objects. We're storing this as JSON initially, and it will be deserialized on demand.
    // See OrderEntity.ShippingLines
    val shipping_lines: JsonElement? = null
    val shipping_total: String? = null
    val status: String? = null
    val total: String? = null
    val total_tax: String? = null
    val meta_data: JsonElement? = null
    val tax_lines: JsonElement? = null
}

fun OrderDto.toDomainModel(localSiteId: LocalId): OrderEntity {
    fun convertDateToUTCString(date: String?): String =
            date?.let { DateUtils.formatGmtAsUtcDateString(it) } ?: "" // Store the date in UTC format

    return OrderEntity(
            orderId = this.id ?: 0,
            localSiteId = localSiteId,
            number = this.number ?: (this.id ?: 0).toString(),
            status = this.status ?: "",
            currency = this.currency ?: "",
            orderKey = this.order_key ?: "",
            dateCreated = convertDateToUTCString(this.date_created_gmt),
            dateModified = convertDateToUTCString(this.date_modified_gmt),
            total = this.total ?: "",
            totalTax = this.total_tax ?: "",
            shippingTotal = this.shipping_total ?: "",
            paymentMethod = this.payment_method ?: "",
            paymentMethodTitle = this.payment_method_title ?: "",
            datePaid = this.date_paid_gmt?.let { "${it}Z" } ?: "",
            pricesIncludeTax = this.prices_include_tax,
            customerNote = this.customer_note ?: "",
            discountTotal = this.discount_total ?: "",
            discountCodes = this.coupon_lines?.let { couponLines ->
                // Extract the discount codes from the coupon_lines list and store them as a comma-delimited String
                couponLines
                        .filter { !it.code.isNullOrEmpty() }
                        .joinToString { it.code!! }
            }.orEmpty(),
            refundTotal = this.refunds?.let { refunds ->
                // Extract the individual refund totals from the refunds list and store their sum as a Double,
                refunds.sumOf { it.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO }
            } ?: BigDecimal.ZERO,
            billingFirstName = this.billing?.first_name ?: "",
            billingLastName = this.billing?.last_name ?: "",
            billingCompany = this.billing?.company ?: "",
            billingAddress1 = this.billing?.address_1 ?: "",
            billingAddress2 = this.billing?.address_2 ?: "",
            billingCity = this.billing?.city ?: "",
            billingState = this.billing?.state ?: "",
            billingPostcode = this.billing?.postcode ?: "",
            billingCountry = this.billing?.country ?: "",
            billingEmail = this.billing?.email ?: "",
            billingPhone = this.billing?.phone ?: "",
            shippingFirstName = this.shipping?.first_name ?: "",
            shippingLastName = this.shipping?.last_name ?: "",
            shippingCompany = this.shipping?.company ?: "",
            shippingAddress1 = this.shipping?.address_1 ?: "",
            shippingAddress2 = this.shipping?.address_2 ?: "",
            shippingCity = this.shipping?.city ?: "",
            shippingState = this.shipping?.state ?: "",
            shippingPostcode = this.shipping?.postcode ?: "",
            shippingCountry = this.shipping?.country ?: "",
            lineItems = this.line_items.toString(),
            shippingLines = this.shipping_lines.toString(),
            feeLines = this.fee_lines.toString(),
            taxLines = this.tax_lines.toString(),
            metaData = this.meta_data.toString()
    )
}
