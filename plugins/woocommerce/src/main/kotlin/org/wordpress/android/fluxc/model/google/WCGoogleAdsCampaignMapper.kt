package org.wordpress.android.fluxc.model.google

import org.wordpress.android.fluxc.network.rest.wpcom.wc.google.WCGoogleAdsCampaignDTO
import org.wordpress.android.util.AppLog
import javax.inject.Inject

class WCGoogleAdsCampaignMapper @Inject constructor() {
    fun mapToModel(dto: WCGoogleAdsCampaignDTO): WCGoogleAdsCampaign {
        return WCGoogleAdsCampaign(
            id = dto.id,
            name = dto.name,
            status = dto.status?.let {
                try {
                    WCGoogleAdsCampaign.Status.valueOf(it.uppercase())
                } catch (e: IllegalArgumentException) {
                    AppLog.w(
                        AppLog.T.API,
                        "Unknown campaign status returned: `$it`, defaulting to DISABLED " +
                        e.message
                    )
                    WCGoogleAdsCampaign.Status.DISABLED
                }
            },
            type = dto.rawType,
            amount = dto.amount,
            country = dto.country,
            targetedLocations = dto.targetedLocations
        )
    }
}
