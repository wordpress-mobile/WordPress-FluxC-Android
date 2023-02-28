package org.wordpress.android.fluxc.model.subscription

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.subscription.SubscriptionRestClient
import org.wordpress.android.fluxc.utils.DateUtils
import javax.inject.Inject

class WCSubscriptionMapper @Inject constructor() {
    fun map(dto: SubscriptionRestClient.SubscriptionDto, orderId: Long, site: SiteModel): WCSubscriptionModel {
        return WCSubscriptionModel(
            localSiteId = site.siteId,
            orderId = orderId,
            subscriptionId = dto.id ?: 0L,
            status = dto.status ?: "",
            billingPeriod = dto.billing_period ?: "",
            billingInterval = dto.billing_interval?.toIntOrNull() ?: 0,
            total = dto.total ?: "",
            startDate = dto.start_date_gmt?.let { DateUtils.formatGmtAsUtcDateString(it) } ?: "",
            currency = dto.currency?: ""
        )
    }
}
