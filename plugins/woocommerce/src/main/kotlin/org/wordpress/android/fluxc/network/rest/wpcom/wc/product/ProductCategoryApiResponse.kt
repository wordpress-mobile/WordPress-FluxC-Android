package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.network.Response

@Suppress("PropertyName")
class ProductCategoryApiResponse : Response {
    val id: Long = 0L
    var localSiteId = 0
    var name: String? = null
    var slug: String? = null
    var parent: Long? = null

    fun asProductCategoryModel(): WCProductCategoryModel {
        val response = this
        return WCProductCategoryModel().apply {
            remoteCategoryId = response.id
            name = response.name ?: ""
            slug = response.slug ?: ""
            parent = response.parent ?: 0L
        }
    }
}
