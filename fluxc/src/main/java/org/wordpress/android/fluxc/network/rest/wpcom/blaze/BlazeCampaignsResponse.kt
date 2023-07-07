package org.wordpress.android.fluxc.network.rest.wpcom.blaze

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignsModel

data class AudienceList(
    @SerializedName("OSs") val oSS: String?,
    val countries: String?,
    val devices: String?,
    val languages: String?,
    val topics: String?
)

data class ContentConfig(
    val clickUrl: String?,
    val imageUrl: String?,
    val snippet: String?,
    val title: String
)

data class Campaign(
    @SerializedName("alt_text") val altText: String?,
    @SerializedName("audience_list") val audienceList: AudienceList?,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("budget_cents") val budgetCents: Long?,
    @SerializedName("campaign_id") val campaignId: Int?,
    @SerializedName("clicks") val clicks: String?,
    @SerializedName("content_config") val contentConfig: ContentConfig,
    @SerializedName("content_image") val contentImage: String?,
    @SerializedName("content_target_iab_category") val contentTargetIabCategory: String?,
    @SerializedName("content_target_language") val contentTargetLanguage: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("creative_asset_id") val creativeAssetId: Int?,
    @SerializedName("creative_html") val creativeHtml: String?,
    @SerializedName("delivery_percent") val deliveryPercent: Int?,
    @SerializedName("description") val description: String?,
    @SerializedName("device_target_type") val deviceTargetType: String?,
    @SerializedName("display_delivery_estimate") val displayDeliveryEstimate: String?,
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("end_date") val endDate: String?,
    @SerializedName("file_name") val fileName: String?,
    @SerializedName("height") val height: Int?,
    @SerializedName("image_mime_type") val imageMimeType: String?,
    @SerializedName("impressions") val impressions: String?,
    @SerializedName("keyword_target_ids") val keywordTargetIds: String?,
    @SerializedName("keyword_target_kvs") val keywordTargetKvs: String?,
    @SerializedName("mime_type") val mimeType: String?,
    @SerializedName("moderation_reason") val moderationReason: String?,
    @SerializedName("moderation_status") val moderationStatus: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("os_target_type") val osTargetType: String?,
    @SerializedName("owner_id") val ownerId: Int?,
    @SerializedName("page_names") val pageNames: String?,
    @SerializedName("placement") val placement: String?,
    @SerializedName("revenue") val revenue: String?,
    @SerializedName("site_names") val siteNames: String?,
    @SerializedName("smart_delivery_estimate") val smartDeliveryEstimate: String?,
    @SerializedName("smart_id") val smartId: String?,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("status") val status: String?,
    @SerializedName("status_smart") val statusSmart: Int?,
    @SerializedName("subscription_id") val subscriptionId: Int?,
    @SerializedName("target_url") val targetUrl: String?,
    @SerializedName("target_urn") val targetUrn: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("ui_status") val uiStatus: String,
    @SerializedName("user_target_geo") val userTargetGeo: String?,
    @SerializedName("user_target_geo2") val userTargetGeo2: String?,
    @SerializedName("user_target_language") val userTargetLanguage: String?,
    @SerializedName("width") val width: Int?
) {
    fun toCampaignsModel(): BlazeCampaignModel {
        return BlazeCampaignModel(
            campaignId = campaignId?.toLong() ?: 0,
            title = contentConfig.title,
            imageUrl = contentConfig.imageUrl,
            startDate = BlazeCampaignsUtils.stringToDate(startDate),
            endDate = endDate?.let { BlazeCampaignsUtils.stringToDate(it) },
            uiStatus = uiStatus,
            budgetCents = budgetCents?:0,
            impressions = impressions?.toLong() ?: 0,
            clicks = clicks?.toLong() ?: 0
        )
    }
}

data class BlazeCampaignsResponse(
    @SerializedName("campaigns") val campaigns: List<Campaign>,
    @SerializedName("page") val page: Int,
    @SerializedName("total_items") val totalItems: Int,
    @SerializedName("total_pages") val totalPages: Int
) {
    fun toCampaignsModel() = BlazeCampaignsModel(
        campaigns = campaigns.map { it.toCampaignsModel() },
        page = page,
        totalItems = totalItems,
        totalPages = totalPages
    )
}
