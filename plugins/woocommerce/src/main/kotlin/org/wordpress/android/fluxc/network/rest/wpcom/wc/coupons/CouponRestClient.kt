package org.wordpress.android.fluxc.network.rest.wpcom.wc.coupons

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.coupon.UpdateCouponRequest
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import org.wordpress.android.fluxc.persistence.entity.CouponEntity
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
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
        val url = WOOCOMMERCE.coupons.pathV3

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

    suspend fun createCoupon(
        site: SiteModel,
        request: UpdateCouponRequest
    ): WooPayload<CouponEntity> {
        val url = WOOCOMMERCE.orders.pathV3
        val params = request.toNetworkRequest()

        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
            this,
            site,
            url,
            params,
            CouponDto::class.java
        )

        return when (response) {
            is JetpackError -> WooPayload(response.error.toWooError())
            is JetpackSuccess -> response.data?.let { couponDto ->
                WooPayload(couponDto.toDataModel(site.siteId))
            } ?: WooPayload(
                error = WooError(
                    type = GENERIC_ERROR,
                    original = UNKNOWN,
                    message = "Success response with empty data"
                )
            )
        }
    }

    suspend fun deleteCoupon(
        site: SiteModel,
        couponId: Long,
        trash: Boolean
    ): WooPayload<Unit> {
        val url = WOOCOMMERCE.coupons.id(couponId).pathV3

        val response = jetpackTunnelGsonRequestBuilder.syncDeleteRequest(
            restClient = this,
            site = site,
            url = url,
            clazz = Unit::class.java,
            params = mapOf("force" to trash.not().toString())
        )

        return when (response) {
            is JetpackError -> WooPayload(response.error.toWooError())
            is JetpackSuccess -> WooPayload(Unit)
        }
    }


    private fun UpdateCouponRequest.toNetworkRequest(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            code?.let { put("code", it) }
            discountType?.let { put("discount_type", it) }
            description?.let { put("description", it) }
            dateExpires?.let { put("date_expires", it) }
            dateExpiresGmt?.let { put("date_expires_gmt", it) }
            usageCount?.let { put("usage_count", it) }
            minimumAmount?.let { put("minimum_amount", it) }
            maximumAmount?.let { put("maximum_amount", it) }
            productIds?.let { put("product_ids", it) }
            excludedProductIds?.let { put("excluded_product_ids", it) }
            isShippingFree?.let { put("free_shipping", it) }
            productCategoryIds?.let { put("product_categories", it) }
            excludedProductCategoryIds?.let { put("excluded_product_categories", it) }
            usageLimit?.let { put("usage_limit", it) }
            usageLimitPerUser?.let { put("usage_limit_per_user", it) }
            limitUsageToXItems?.let { put("limit_usage_to_x_items", it) }
            restrictedEmails?.let { put("email_restrictions", it) }
            isForIndividualUse?.let { put("individual_use", it) }
            areSaleItemsExcluded?.let { put("exclude_sale_items", it) }
            usedBy?.let { put("used_by", it) }
        }
    }
}
