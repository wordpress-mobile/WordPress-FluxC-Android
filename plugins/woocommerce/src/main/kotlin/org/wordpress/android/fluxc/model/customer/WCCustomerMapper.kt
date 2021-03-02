package org.wordpress.android.fluxc.model.customer

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerApiResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCCustomerMapper @Inject constructor() {
    fun map(site: SiteModel, resp: CustomerApiResponse): WCCustomerModel {
        return WCCustomerModel().apply {
            localSiteId = site.id
            avatarUrl = resp.avatarUrl ?: ""
            dateCreated = resp.dateCreated ?: ""
            dateCreatedGmt = resp.dateCreatedGmt ?: ""
            dateModified = resp.dateModified ?: ""
            dateModifiedGmt = resp.dateModifiedGmt ?: ""
            email = resp.email ?: ""
            firstName = resp.firstName ?: ""
            remoteCustomerId = resp.id ?: 0
            isPayingCustomer = resp.isPayingCustomer
            lastName = resp.lastName ?: ""
            role = resp.role ?: ""
            username = resp.username ?: ""

            billingAddress1 = resp.billing?.address1 ?: ""
            billingAddress2 = resp.billing?.address2 ?: ""
            billingCompany = resp.billing?.company ?: ""
            billingCountry = resp.billing?.country ?: ""
            billingCity = resp.billing?.city ?: ""
            billingEmail = resp.billing?.email ?: ""
            billingFirstName = resp.billing?.firstName ?: ""
            billingLastName = resp.billing?.lastName ?: ""
            billingPhone = resp.billing?.phone ?: ""
            billingPostcode = resp.billing?.postcode ?: ""
            billingState = resp.billing?.state ?: ""

            shippingAddress1 = resp.billing?.address1 ?: ""
            shippingAddress2 = resp.billing?.address2 ?: ""
            shippingCity = resp.billing?.city ?: ""
            shippingCompany = resp.billing?.company ?: ""
            shippingCountry = resp.billing?.country ?: ""
            shippingFirstName = resp.billing?.firstName ?: ""
            shippingLastName = resp.billing?.lastName ?: ""
            shippingPostcode = resp.billing?.postcode ?: ""
            shippingState = resp.billing?.state ?: ""
        }
    }
}
