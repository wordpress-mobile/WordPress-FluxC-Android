package org.wordpress.android.fluxc.network.rest.wpcom.wc.google

data class GoogleAdsCampaign(
    val id: Long,
    val name: String
)

data class GoogleAdsInterval(
    val interval: String
)

data class GoogleAdsTotals(
    val sales: Double,
    val spend: Double
)