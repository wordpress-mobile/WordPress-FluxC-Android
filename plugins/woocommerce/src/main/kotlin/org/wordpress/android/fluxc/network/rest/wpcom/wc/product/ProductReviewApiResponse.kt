package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.JsonElement
import org.wordpress.android.fluxc.network.Response

@Suppress("PropertyName")
class ProductReviewApiResponse : Response {
    val id: Long? = null
    val date_created: String? = null
    val product_id: Long? = null
    val status: String? = null
    val reviewer: String? = null
    val reviewer_email: String? = null
    val review: String? = null
    val rating: Int? = null
    val verified: Boolean? = null
    val reviewer_avatar_urls: JsonElement? = null
}
