package org.wordpress.android.fluxc.model.customer

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerDTO
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCCustomerMapper @Inject constructor() {
    @Suppress("ComplexMethod")
    fun mapToModel(site: SiteModel, dto: CustomerDTO): WCCustomerModel {
        return WCCustomerModel().apply {
            localSiteId = site.id
            avatarUrl = dto.avatarUrl ?: ""
            dateCreated = dto.dateCreated ?: ""
            dateCreatedGmt = dto.dateCreatedGmt ?: ""
            dateModified = dto.dateModified ?: ""
            dateModifiedGmt = dto.dateModifiedGmt ?: ""
            email = dto.email ?: ""
            firstName = dto.firstName ?: ""
            remoteCustomerId = dto.id ?: 0
            isPayingCustomer = dto.isPayingCustomer
            lastName = dto.lastName ?: ""
            role = dto.role ?: ""
            username = dto.username ?: ""

            billingAddress1 = dto.billing?.address1 ?: ""
            billingAddress2 = dto.billing?.address2 ?: ""
            billingCompany = dto.billing?.company ?: ""
            billingCountry = dto.billing?.country ?: ""
            billingCity = dto.billing?.city ?: ""
            billingEmail = dto.billing?.email ?: ""
            billingFirstName = dto.billing?.firstName ?: ""
            billingLastName = dto.billing?.lastName ?: ""
            billingPhone = dto.billing?.phone ?: ""
            billingPostcode = dto.billing?.postcode ?: ""
            billingState = dto.billing?.state ?: ""

            shippingAddress1 = dto.billing?.address1 ?: ""
            shippingAddress2 = dto.billing?.address2 ?: ""
            shippingCity = dto.billing?.city ?: ""
            shippingCompany = dto.billing?.company ?: ""
            shippingCountry = dto.billing?.country ?: ""
            shippingFirstName = dto.billing?.firstName ?: ""
            shippingLastName = dto.billing?.lastName ?: ""
            shippingPostcode = dto.billing?.postcode ?: ""
            shippingState = dto.billing?.state ?: ""
        }
    }

    fun mapToDTO(model: WCCustomerModel): CustomerDTO {
        val billing = CustomerDTO.Billing(
                firstName = model.billingFirstName,
                lastName = model.billingLastName,
                company = model.billingCompany,
                address1 = model.billingAddress1,
                address2 = model.billingAddress2,
                city = model.billingCity,
                state = model.billingState,
                postcode = model.billingPostcode,
                country = model.billingCountry,
                email = model.billingEmail,
                phone = model.billingPhone
        )
        val shipping = CustomerDTO.Shipping(
                firstName = model.billingFirstName,
                lastName = model.billingLastName,
                company = model.billingCompany,
                address1 = model.billingAddress1,
                address2 = model.billingAddress2,
                city = model.billingCity,
                state = model.billingState,
                postcode = model.billingPostcode,
                country = model.billingCountry
        )
        return CustomerDTO(
                email = model.email,
                firstName = model.firstName,
                lastName = model.lastName,
                username = model.username,
                billing = billing,
                shipping = shipping
        )
    }
}
