package org.wordpress.android.fluxc.model.customer

import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerApiResponse
import kotlin.test.assertEquals

class WCCustomerMapperTest {
    val mapper = WCCustomerMapper()

    @Test
    fun `mapper maps to correct customer model`() {
        // given
        val remoteId = 13L
        val siteId = 23
        val site = SiteModel().apply { id = siteId }

        val billing = CustomerApiResponse.Billing(
                address1 = "address1",
                address2 = "address2",
                city = "city",
                company = "company",
                country = "country",
                email = "email",
                firstName = "firstName",
                lastName = "lastName",
                phone = "phone",
                postcode = "postcode",
                state = "state"
        )
        val shipping = CustomerApiResponse.Shipping(
                address1 = "address1",
                address2 = "address2",
                city = "city",
                company = "company",
                country = "country",
                firstName = "firstName",
                lastName = "lastName",
                postcode = "postcode",
                state = "state"
        )
        val response = CustomerApiResponse(
                avatarUrl = "avatarUrl",
                billing = billing,
                dateCreated = "dateCreated",
                dateCreatedGmt = "dateCreatedGmt",
                dateModified = "dateModified",
                dateModifiedGmt = "dateModifiedGmt",
                email = "email",
                firstName = "firstName",
                id = remoteId,
                isPayingCustomer = true,
                lastName = "lastName",
                role = "role",
                shipping = shipping,
                username = "username"
        )

        // when
        val result = mapper.map(site, response)

        // then
        with(result) {
            assertEquals("avatarUrl", avatarUrl)
            assertEquals("dateCreated", dateCreated)
            assertEquals("dateCreatedGmt", dateCreatedGmt)
            assertEquals("dateModified", dateModified)
            assertEquals("dateModifiedGmt", dateModifiedGmt)
            assertEquals("email", email)
            assertEquals("firstName", firstName)
            assertEquals(remoteCustomerId, remoteCustomerId)
            assertEquals(true, isPayingCustomer)
            assertEquals("lastName", lastName)
            assertEquals("role", role)
            assertEquals("username", username)

            assertEquals("address1", shippingAddress1)
            assertEquals("address2", shippingAddress2)
            assertEquals("city", shippingCity)
            assertEquals("company", shippingCompany)
            assertEquals("country", shippingCountry)
            assertEquals("firstName", shippingFirstName)
            assertEquals("lastName", shippingLastName)
            assertEquals("postcode", shippingPostcode)
            assertEquals("state", shippingState)

            assertEquals("address1", billingAddress1)
            assertEquals("address2", billingAddress2)
            assertEquals("city", billingCity)
            assertEquals("company", billingCompany)
            assertEquals("country", billingCountry)
            assertEquals("firstName", billingFirstName)
            assertEquals("lastName", billingLastName)
            assertEquals("postcode", billingPostcode)
            assertEquals("state", billingState)
        }
    }
}
