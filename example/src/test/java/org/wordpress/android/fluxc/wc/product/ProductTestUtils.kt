package org.wordpress.android.fluxc.wc.product

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductShippingClassModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductCategoryApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductReviewApiResponse

object ProductTestUtils {
    fun generateSampleProduct(
        remoteId: Long,
        type: String = "simple",
        name: String = "",
        virtual: Boolean = false,
        siteId: Int = 6,
        stockStatus: String = CoreProductStockStatus.IN_STOCK.value,
        status: String = "publish"
    ): WCProductModel {
        return WCProductModel().apply {
            remoteProductId = remoteId
            localSiteId = siteId
            this.type = type
            this.name = name
            this.virtual = virtual
            this.stockStatus = stockStatus
            this.status = status
        }
    }

    fun generateSampleProductShippingClass(
        remoteId: Long = 1L,
        name: String = "",
        slug: String = "",
        description: String = "",
        siteId: Int = 6
    ): WCProductShippingClassModel {
        return WCProductShippingClassModel().apply {
            remoteShippingClassId = remoteId
            localSiteId = siteId
            this.name = name
            this.slug = slug
            this.description = description
        }
    }

    fun generateProductList(siteId: Int = 6): List<WCProductShippingClassModel> {
        with(ArrayList<WCProductShippingClassModel>()) {
            add(generateSampleProductShippingClass(1, siteId = siteId))
            add(generateSampleProductShippingClass(2, siteId = siteId))
            add(generateSampleProductShippingClass(3, siteId = siteId))
            add(generateSampleProductShippingClass(4, siteId = siteId))
            add(generateSampleProductShippingClass(5, siteId = siteId))
            return this
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

    fun getProductCategoriesFromJsonString(json: String, siteId: Int): List<WCProductCategoryModel> {
        val responseType = object : TypeToken<List<ProductCategoryApiResponse>>() {}.type
        val converted = Gson().fromJson(json, responseType) as? List<ProductCategoryApiResponse> ?: emptyList()
        return converted.map {
            WCProductCategoryModel().apply {
                localSiteId = siteId
                remoteCategoryId = it.id ?: 0L
                name = it.name ?: ""
                slug = it.slug ?: ""
                parent = it.parent ?: 0L
            }
        }
    }
}
