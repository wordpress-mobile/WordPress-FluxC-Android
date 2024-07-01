package org.wordpress.android.fluxc.model.google

data class WCGoogleAdsCampaign(
    val id: Long?,
    val name: String?,
    val status: Status?,
    val type: String?,
    val amount: Double?,
    val country: String?,
    val targetedLocations: List<String>?
) {
    enum class Status {
        ENABLED,
        DISABLED,
        REMOVED
    }
}
