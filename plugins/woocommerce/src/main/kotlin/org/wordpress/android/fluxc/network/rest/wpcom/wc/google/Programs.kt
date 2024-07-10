package org.wordpress.android.fluxc.network.rest.wpcom.wc.google

data class GoogleAdsProgramsResponse(
    val campaigns: List<GoogleAdsCampaign>,
    val intervals: List<GoogleAdsInterval>,
    val totals: GoogleAdsTotals
)

data class GoogleAdsCampaign(
    val id: Long,
    val name: String,
    val status: String,
    val subtotals: GoogleAdsTotals
)

data class GoogleAdsInterval(
    val interval: String,
    val subtotals: GoogleAdsTotals
)

data class GoogleAdsTotals(
    val sales: Double,
    val spend: Double
)
