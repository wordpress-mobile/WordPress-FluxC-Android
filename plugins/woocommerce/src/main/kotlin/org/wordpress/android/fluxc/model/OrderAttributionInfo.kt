package org.wordpress.android.fluxc.model

import org.wordpress.android.fluxc.persistence.entity.OrderMetaDataEntity

data class OrderAttributionInfo(
    val sourceType: String? = null,
    val campaign: String? = null,
    val source: String? = null,
    val medium: String? = null,
    val deviceType: String? = null,
    val sessionPageViews: String? = null
) {
    constructor(metadata: List<OrderMetaDataEntity>) : this(
        sourceType = metadata.find { it.key == WCMetaData.OrderAttributionInfoKeys.SOURCE_TYPE }?.value,
        campaign = metadata.find { it.key == WCMetaData.OrderAttributionInfoKeys.CAMPAIGN }?.value,
        source = metadata.find { it.key == WCMetaData.OrderAttributionInfoKeys.SOURCE }?.value,
        medium = metadata.find { it.key == WCMetaData.OrderAttributionInfoKeys.MEDIUM }?.value,
        deviceType = metadata.find { it.key == WCMetaData.OrderAttributionInfoKeys.DEVICE_TYPE }?.value,
        sessionPageViews = metadata.find {
            it.key == WCMetaData.OrderAttributionInfoKeys.SESSION_PAGE_VIEWS
        }?.value
    )
}
