package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.model.order.OrderAddress.AddressType
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCOrderModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column var remoteOrderId = 0L
    @Column var status = ""
    @Column var currency = ""
    @Column var dateCreated = "" // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @Column var total = ""

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


    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }

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
}
