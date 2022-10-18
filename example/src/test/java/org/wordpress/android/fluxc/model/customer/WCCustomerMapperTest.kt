package org.wordpress.android.fluxc.model.customer

import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerDTO
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WCCustomerMapperTest {
    val mapper = WCCustomerMapper()

    @Test
    @Suppress("LongMethod")
    fun `mapper maps to correct customer model`() {
        // given
        val remoteId = 13L
        val siteId = 23
        val site = SiteModel().apply { id = siteId }

        val billing = CustomerDTO.Billing(
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
        val shipping = CustomerDTO.Shipping(
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
        val response = CustomerDTO(
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
        val result = mapper.mapToModel(site, response)

        // then
        with(result) {
            assertEquals("avatarUrl", avatarUrl)
            assertEquals("dateCreated", dateCreated)
            assertEquals("dateCreatedGmt", dateCreatedGmt)
            assertEquals("dateModified", dateModified)
            assertEquals("dateModifiedGmt", dateModifiedGmt)
            assertEquals("email", email)
            assertEquals("firstName", firstName)
            assertEquals(remoteCustomerId, remoteId)
            assertTrue(isPayingCustomer)
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
            assertEquals("email", billingEmail)
            assertEquals("phone", billingPhone)
        }
    }

    @Test
    @Suppress("LongMethod")
    fun `mapper maps to correct customer dto`() {
        // given
        val model = WCCustomerModel().apply {
            avatarUrl = "avatarUrl"
            dateCreated = "dateCreated"
            dateCreatedGmt = "dateCreatedGmt"
            dateModified = "dateModified"
            dateModifiedGmt = "dateModifiedGmt"
            email = "email"
            firstName = "firstName"
            isPayingCustomer = true
            lastName = "lastName"
            role = "role"
            username = "username"

            billingAddress1 = "address1"
            billingAddress2 = "address2"
            billingCity = "city"
            billingCompany = "company"
            billingCountry = "country"
            billingEmail = "email"
            billingFirstName = "firstName"
            billingLastName = "lastName"
            billingPhone = "phone"
            billingPostcode = "postcode"
            billingState = "state"

            shippingAddress1 = "address1"
            shippingAddress2 = "address2"
            shippingCity = "city"
            shippingCompany = "company"
            shippingCountry = "country"
            shippingFirstName = "firstName"
            shippingLastName = "lastName"
            shippingPostcode = "postcode"
            shippingState = "state"
        }

        // when
        val result = mapper.mapToDTO(model)

        // then
        with(result) {
            assertNull(avatarUrl)
            assertNull(dateCreated)
            assertNull(dateCreatedGmt)
            assertNull(dateModified)
            assertNull(dateModifiedGmt)
            assertEquals("email", email)
            assertEquals("firstName", firstName)
            assertFalse(isPayingCustomer)
            assertEquals("lastName", lastName)
            assertNull(role)
            assertEquals("username", username)

            assertEquals("address1", shipping?.address1)
            assertEquals("address2", shipping?.address2)
            assertEquals("city", shipping?.city)
            assertEquals("company", shipping?.company)
            assertEquals("country", shipping?.country)
            assertEquals("firstName", shipping?.firstName)
            assertEquals("lastName", shipping?.lastName)
            assertEquals("postcode", shipping?.postcode)
            assertEquals("state", shipping?.state)

            assertEquals("address1", billing?.address1)
            assertEquals("address2", billing?.address2)
            assertEquals("city", billing?.city)
            assertEquals("company", billing?.company)
            assertEquals("country", billing?.country)
            assertEquals("firstName", billing?.firstName)
            assertEquals("lastName", billing?.lastName)
            assertEquals("postcode", billing?.postcode)
            assertEquals("state", billing?.state)
            assertEquals("email", billing?.email)
            assertEquals("phone", billing?.phone)
        }
    }
}
