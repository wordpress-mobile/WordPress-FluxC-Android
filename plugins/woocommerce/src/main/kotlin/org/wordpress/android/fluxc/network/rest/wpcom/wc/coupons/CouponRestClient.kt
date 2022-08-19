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
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
        pageSize: Int,
        searchQuery: String? = null
    ): WooPayload<Array<CouponDto>> {
        val url = WOOCOMMERCE.coupons.pathV3

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
            restClient = this,
            site = site,
            url = url,
            params = mutableMapOf<String, String>().apply {
                put("page", page.toString())
                put("per_page", pageSize.toString())
                searchQuery?.let {
                    put("search", searchQuery)
                }
            },
            clazz = Array<CouponDto>::class.java
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

    suspend fun fetchCoupon(
        site: SiteModel,
        couponId: Long
    ): WooPayload<CouponDto> {
        val url = WOOCOMMERCE.coupons.id(couponId).pathV3

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
            this,
            site,
            url,
            emptyMap(),
            CouponDto::class.java
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
    ): WooPayload<CouponDto> {
        val url = WOOCOMMERCE.coupons.pathV3
        val params = request.toNetworkRequest()

        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
            this,
            site,
            url,
            params,
            CouponDto::class.java
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

    suspend fun updateCoupon(
        site: SiteModel,
        couponId: Long,
        request: UpdateCouponRequest
    ): WooPayload<CouponDto> {
        val url = WOOCOMMERCE.coupons.id(couponId).pathV3
        val params = request.toNetworkRequest()

        val response = jetpackTunnelGsonRequestBuilder.syncPutRequest(
            this,
            site,
            url,
            params,
            CouponDto::class.java
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

    suspend fun fetchCouponsReports(
        site: SiteModel,
        couponsIds: LongArray = longArrayOf(),
        after: Date
    ): WooPayload<List<CouponReportDto>> {
        val url = WOOCOMMERCE.reports.coupons.pathV4Analytics
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT)

        val params = mapOf(
            "after" to dateFormatter.format(after),
            "coupons" to couponsIds.joinToString(",")
        )

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
            restClient = this,
            site = site,
            url = url,
            params = params,
            clazz = Array<CouponReportDto>::class.java
        )

        return when (response) {
            is JetpackError -> WooPayload(response.error.toWooError())
            is JetpackSuccess -> WooPayload(response.data?.toList())
        }
    }

    suspend fun fetchCouponReport(
        site: SiteModel,
        couponId: Long,
        after: Date
    ): WooPayload<CouponReportDto> {
        return fetchCouponsReports(
            site = site,
            couponsIds = longArrayOf(couponId),
            after = after
        ).let {
            when {
                it.isError -> WooPayload(it.error)
                it.result.isNullOrEmpty() -> WooPayload(
                    WooError(API_ERROR, UNKNOWN, "Empty coupons report response")
                )
                else -> WooPayload(it.result.first())
            }
        }
    }

    @Suppress("ComplexMethod")
    private fun UpdateCouponRequest.toNetworkRequest(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            code?.let { put("code", it) }
            amount?.let { put("amount", it) }
            discountType?.let { put("discount_type", it) }
            description?.let { put("description", it) }
            expiryDate?.let { put("date_expires", it) }
            minimumAmount?.let { put("minimum_amount", it) }
            maximumAmount?.let { put("maximum_amount", it) }
            productIds?.let { put("product_ids", it) }
            excludedProductIds?.let { put("excluded_product_ids", it) }
            isShippingFree?.let { put("free_shipping", it) }
            productCategoryIds?.let { put("product_categories", it) }
            excludedProductCategoryIds?.let { put("excluded_product_categories", it) }
            restrictedEmails?.let { put("email_restrictions", it) }
            isForIndividualUse?.let { put("individual_use", it) }
            areSaleItemsExcluded?.let { put("exclude_sale_items", it) }

            // The following fields can have empty value. When updating a Coupon,
            // the REST API accepts and treats `0` as empty for these fields.
            put("usage_limit", usageLimit ?: 0)
            put("usage_limit_per_user", usageLimitPerUser ?: 0)
            put("limit_usage_to_x_items", limitUsageToXItems ?: 0)
        }
    }
}
