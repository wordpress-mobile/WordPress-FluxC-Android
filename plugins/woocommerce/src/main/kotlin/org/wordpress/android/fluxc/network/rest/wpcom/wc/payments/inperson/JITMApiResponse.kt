package org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.inperson

import com.google.gson.annotations.SerializedName

data class JITMApiResponse(
    @SerializedName("content") val content: JITMContent,
    @SerializedName("CTA") val cta: JITMCta,
    @SerializedName("ttl") val timeToLive: Int,
    @SerializedName("id") val id: String,
    @SerializedName("feature_class") val featureClass: String,
    @SerializedName("expires") val expires: Long,
    @SerializedName("max_dismissal") val maxDismissal: Int,
    @SerializedName("is_dismissible") val isDismissible: Boolean,
    @SerializedName("url") val url: String,
    @SerializedName("jitm_stats_url") val jitmStatsUrl: String,
)

data class JITMContent(
    @SerializedName("message") val message: String,
    @SerializedName("icon") val icon: String,
    @SerializedName("description") val description: String,
    @SerializedName("title") val title: String,
)

data class JITMCta(
    @SerializedName("message") val message: String,
    @SerializedName("link") val link: String,
)