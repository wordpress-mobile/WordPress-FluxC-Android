package org.wordpress.android.fluxc.network.rest.wpcom.wc.coupons

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import javax.inject.Inject
import javax.inject.Named

class CouponRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchCoupons(
        site: SiteModel,
        page: Int,
        pageSize: Int
    ): WooPayload<Array<CouponDto>> {
        val url = WOOCOMMERCE.customers.pathV3

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                mapOf(
                    "page" to page.toString(),
                    "per_page" to pageSize.toString()
                ),
                Array<CouponDto>::class.java
        )
        return when (response) {
            is JetpackSuccess -> {
                WooPayload(response.data)
            }
            is JetpackError -> {
                WooPayload(response.error.toWooError())
            }
        }
    }

    data class CouponDto(
        @SerializedName("id") val id: Long,
        @SerializedName("code") val code: String,
        @SerializedName("date_created") val dateCreated: String?,
        @SerializedName("date_created_gmt") val dateCreatedGmt: String?,
        @SerializedName("date_modified") val dateModified: String?,
        @SerializedName("date_modified_gmt") val dateModifiedGmt: String?,
        @SerializedName("discount_type") val discountType: String?,
        @SerializedName("description") val description: String?,
        @SerializedName("date_expires") val dateExpires: String?,
        @SerializedName("date_expires_gmt") val dateExpiresGmt: String?,
        @SerializedName("usage_count") val usageCount: Int?,
        @SerializedName("individual_use") val individualUse: Boolean?,
        @SerializedName("product_ids") val productIds: List<Long>?,
        @SerializedName("excluded_product_ids") val excludedProductIds: List<Long>?,
        @SerializedName("usage_limit") val usageLimit: Int?,
        @SerializedName("usage_limit_per_user") val usageLimitPerUser: Int?,
        @SerializedName("limit_usage_to_x_items") val limitUsageToXItems: Int?,
        @SerializedName("free_shipping") val freeShipping: Boolean?,
        @SerializedName("product_categories") val productCategories: List<Long>?,
        @SerializedName("excluded_product_categories") val excludedProductCategories: List<Long>?,
        @SerializedName("exclude_sale_items") val excludeSaleItems: Boolean?,
        @SerializedName("minimum_amount") val minimumAmount: String?,
        @SerializedName("maximum_amount") val maximumAmount: String?,
        @SerializedName("email_restrictions") val emailRestrictions: List<String>?,
        @SerializedName("used_by") val usedBy: List<Long>?
    )
}
