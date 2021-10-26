package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import org.wordpress.android.fluxc.model.order.OrderAddress

object OrderDtoMapper {
    fun OrderAddress.Billing.toDto() = OrderDto.Billing(
            first_name = this.firstName,
            last_name = this.lastName,
            company = this.company,
            address_1 = this.address1,
            address_2 = this.address2,
            city = this.city,
            state = this.state,
            postcode = this.postcode,
            country = this.country,
            email = this.email,
            phone = this.phone
    )

    fun OrderAddress.Shipping.toDto() = OrderDto.Shipping(
            first_name = this.firstName,
            last_name = this.lastName,
            company = this.company,
            address_1 = this.address1,
            address_2 = this.address2,
            city = this.city,
            state = this.state,
            postcode = this.postcode,
            country = this.country,
            phone = this.phone
    )
}
