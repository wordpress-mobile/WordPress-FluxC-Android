package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

/**
 * Single Woo customer - see https://woocommerce.github.io/woocommerce-rest-api-docs/#customer-properties
 */
@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCCustomerModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var avatarUrl: String? = null
    @Column var dateCreated: String? = null
    @Column var dateCreatedGmt: String? = null
    @Column var dateModified: String? = null
    @Column var dateModifiedGmt: String? = null
    @Column var email: String? = null
    @Column var firstName: String? = null
    @Column var remoteCustomerId: Long = 0
    @Column var isPayingCustomer: Boolean = false
        @JvmName("setIsPayingCustomer")
        set
    @Column var lastName: String? = null
    @Column var role: String? = null
    @Column var username: String? = null

    @Column var localSiteId = 0

    @Column var billingAddress1: String? = null
    @Column var billingAddress2: String? = null
    @Column var billingCity: String? = null
    @Column var billingCompany: String? = null
    @Column var billingCountry: String? = null
    @Column var billingEmail: String? = null
    @Column var billingFirstName: String? = null
    @Column var billingLastName: String? = null
    @Column var billingPhone: String? = null
    @Column var billingPostcode: String? = null
    @Column var billingState: String? = null

    @Column var shippingAddress1: String? = null
    @Column var shippingAddress2: String? = null
    @Column var shippingCity: String? = null
    @Column var shippingCompany: String? = null
    @Column var shippingCountry: String? = null
    @Column var shippingFirstName: String? = null
    @Column var shippingLastName: String? = null
    @Column var shippingPostcode: String? = null
    @Column var shippingState: String? = null

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }
}
