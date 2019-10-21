package org.wordpress.android.fluxc.model.refunds

import org.wordpress.android.fluxc.model.refunds.WCRefundModel.WCRefundItem
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.DATE_FORMAT_DAY
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundRestClient.RefundResponse
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class RefundMapper
@Inject constructor() {
    fun map(response: RefundResponse): WCRefundModel {
        return WCRefundModel(
                response.refundId,
                response.dateCreated?.let { fromFormattedDate(it) } ?: Date(),
                response.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                response.reason,
                response.refundedPayment ?: false,
                response.items.map {
                    WCRefundItem(
                            it.id ?: -1,
                            it.name ?: "",
                            it.productId ?: -1,
                            it.variationId ?: -1,
                            it.quantity ?: 0f,
                            it.subtotal?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            it.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            it.totalTax?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            it.sku ?: "",
                            it.price?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    )
                }
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
