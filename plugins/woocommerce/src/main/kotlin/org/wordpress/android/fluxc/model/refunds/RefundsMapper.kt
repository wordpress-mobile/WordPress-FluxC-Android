package org.wordpress.android.fluxc.model.refunds

import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundsRestClient.RefundResponse

class RefundsMapper {
    fun map(response: RefundResponse): RefundModel {
        return RefundModel(response.refundedPayment ?: false)
    }
}
