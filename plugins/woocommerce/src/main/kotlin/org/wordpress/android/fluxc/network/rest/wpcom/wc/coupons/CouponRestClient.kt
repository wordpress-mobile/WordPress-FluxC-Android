package org.wordpress.android.fluxc.network.rest.wpcom.wc.coupons

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
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
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
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
        couponsIds: Array<Long> = emptyArray(),
        after: Date
    ): WooPayload<List<CouponReportDto>> {
        val url = WOOCOMMERCE.reports.coupons.pathV4Analytics

        val params = mapOf(
            "after" to DateTimeUtils.iso8601FromDate(after),
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
            couponsIds = arrayOf(couponId),
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
}
