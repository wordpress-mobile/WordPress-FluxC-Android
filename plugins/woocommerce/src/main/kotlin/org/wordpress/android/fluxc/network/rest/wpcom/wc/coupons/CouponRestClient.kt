package org.wordpress.android.fluxc.network.rest.wpcom.wc.coupons

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.coupon.UpdateCouponRequest
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class CouponRestClient @Inject constructor(
    private val wooNetwork: WooNetwork
) {
    suspend fun fetchCoupons(
        site: SiteModel,
        page: Int,
        pageSize: Int,
        searchQuery: String? = null
    ): WooPayload<Array<CouponDto>> {
        val url = WOOCOMMERCE.coupons.pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
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
            is WPAPIResponse.Success -> {
                WooPayload(response.data)
            }
            is WPAPIResponse.Error -> {
                WooPayload(response.error.toWooError())
            }
        }
    }

    suspend fun fetchCoupon(
        site: SiteModel,
        couponId: Long
    ): WooPayload<CouponDto> {
        val url = WOOCOMMERCE.coupons.id(couponId).pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = CouponDto::class.java
        )
        return when (response) {
            is WPAPIResponse.Success -> {
                WooPayload(response.data)
            }
            is WPAPIResponse.Error -> {
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

        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            clazz = CouponDto::class.java,
            body = params
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                WooPayload(response.data)
            }
            is WPAPIResponse.Error -> {
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

        val response = wooNetwork.executePutGsonRequest(
            site = site,
            path = url,
            clazz = CouponDto::class.java,
            body = params
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                WooPayload(response.data)
            }
            is WPAPIResponse.Error -> {
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

        val response = wooNetwork.executeDeleteGsonRequest(
            site = site,
            path = url,
            clazz = Unit::class.java,
            params = mapOf("force" to trash.not().toString())
        )

        return when (response) {
            is WPAPIResponse.Error -> WooPayload(response.error.toWooError())
            is WPAPIResponse.Success -> WooPayload(Unit)
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

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = Array<CouponReportDto>::class.java
        )

        return when (response) {
            is WPAPIResponse.Error -> WooPayload(response.error.toWooError())
            is WPAPIResponse.Success -> WooPayload(response.data?.toList())
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
