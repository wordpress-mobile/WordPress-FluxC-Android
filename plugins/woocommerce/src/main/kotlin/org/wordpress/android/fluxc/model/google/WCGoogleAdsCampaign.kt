package org.wordpress.android.fluxc.model.google

data class WCGoogleAdsCampaign(
    val id: Long?,
    val name: String?,
    val status: Status?,
    val type: String?,
    val amount: Double?,
    val countryISOCode: String?,
    val targetedCountryISOCodes: List<String>?
) {
    enum class Status {
        ENABLED,
        DISABLED,
        REMOVED
    }
}
