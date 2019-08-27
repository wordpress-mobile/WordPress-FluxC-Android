package org.wordpress.android.fluxc.wc.product

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductReviewApiResponse

object ProductTestUtils {
    fun generateSampleProduct(
        remoteId: Long,
        type: String = "simple",
        name: String = "",
        virtual: Boolean = false,
        siteId: Int = 6
    ): WCProductModel {
        return WCProductModel().apply {
            remoteProductId = remoteId
            localSiteId = siteId
            this.type = type
            this.name = name
            this.virtual = virtual
        }
    }

    fun getProductReviewsFromJsonString(json: String, siteId: Int): List<WCProductReviewModel> {
        val responseType = object : TypeToken<List<ProductReviewApiResponse>>() {}.type
        val converted = Gson().fromJson(json, responseType) as? List<ProductReviewApiResponse> ?: emptyList()
        return converted.map {
            WCProductReviewModel().apply {
                localSiteId = siteId
                remoteProductReviewId = it.id ?: 0L
                remoteProductId = it.product_id ?: 0L
                dateCreated = it.date_created_gmt?.let { "${it}Z" } ?: ""
                status = it.status ?: ""
                reviewerName = it.reviewer ?: ""
                reviewerEmail = it.reviewer_email ?: ""
                review = it.review ?: ""
                rating = it.rating ?: 0
                verified = it.verified ?: false
                reviewerAvatarsJson = it.reviewer_avatar_urls.toString()
            }
        }
    }
}
