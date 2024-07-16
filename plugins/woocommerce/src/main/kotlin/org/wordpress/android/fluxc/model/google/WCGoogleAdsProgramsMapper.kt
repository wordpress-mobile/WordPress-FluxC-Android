package org.wordpress.android.fluxc.model.google

import org.wordpress.android.fluxc.network.rest.wpcom.wc.google.WCGoogleAdsProgramsDTO
import javax.inject.Inject

class WCGoogleAdsProgramsMapper @Inject constructor() {
    fun mapToModel(dto: WCGoogleAdsProgramsDTO): WCGoogleAdsPrograms {
        return WCGoogleAdsPrograms(
            campaigns = dto.campaigns?.map {
                WCGoogleAdsProgramCampaign(
                    id = it.id,
                    name = it.name,
                    status = it.status?.let { status ->
                        WCGoogleAdsCampaign.Status.fromString(status)
                    },
                    subtotal = WCGoogleAdsProgramTotals(
                        sales = it.subtotals?.sales,
                        spend = it.subtotals?.spend
                    )
                )
            },
            intervals = dto.intervals?.map {
                WCGoogleAdsProgramInterval(
                    interval = it.interval,
                    subtotal = WCGoogleAdsProgramTotals(
                        sales = it.subtotals?.sales,
                        spend = it.subtotals?.spend
                    )
                )
            },
            totals = dto.totals?.let {
                WCGoogleAdsProgramTotals(
                    sales = it.sales,
                    spend = it.spend
                )
            }
        )
    }

    fun mapToImpressionsAndClicks(dto: WCGoogleAdsProgramsDTO): Pair<Double, Double> {
        return dto.totals?.let { Pair(it.impressions ?: 0.0, it.clicks ?: 0.0) } ?: Pair(0.0, 0.0)
    }
}
