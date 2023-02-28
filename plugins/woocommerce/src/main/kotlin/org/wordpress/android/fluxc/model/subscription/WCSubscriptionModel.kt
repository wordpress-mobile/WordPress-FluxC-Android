package org.wordpress.android.fluxc.model.subscription

data class WCSubscriptionModel(
    val localSiteId: Long = 0L,
    val orderId: Long = 0L,
    val subscriptionId: Long = 0L,
    val status: String,
    val billingPeriod: String,
    val billingInterval: Int,
    val total: String,
    val startDate: String,
    val currency: String
)
