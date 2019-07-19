package org.wordpress.android.fluxc.model.refunds

import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.DATE_FORMAT_DAY
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundsRestClient.RefundResponse
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class RefundsMapper
@Inject constructor() {
    fun map(response: RefundResponse): RefundModel {
        return RefundModel(
                response.refundId ?: -1,
                response.dateCreated?.let { fromFormattedDate(it) } ?: Date(),
                response.amount?.toBigDecimal() ?: BigDecimal.ZERO,
                response.reason,
                response.refundedPayment ?: false
        )
    }

    private fun fromFormattedDate(date: String): Date? {
        if (date.isEmpty()) {
            return null
        }
        val dateFormat = SimpleDateFormat(DATE_FORMAT_DAY, Locale.ROOT)
        return dateFormat.parse(date)
    }
}
