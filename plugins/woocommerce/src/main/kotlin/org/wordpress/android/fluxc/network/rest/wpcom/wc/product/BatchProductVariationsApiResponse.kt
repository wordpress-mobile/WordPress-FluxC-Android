package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response

/**
 * Representation of Batch Update Product Variations API response.
 */
class BatchProductVariationsApiResponse : Response {
    @SerializedName("create")
    var createdVariations: List<ProductVariationApiResponse>? = null
    @SerializedName("update")
    var updatedVariations: List<ProductVariationApiResponse>? = null
    @SerializedName("delete")
    var deletedVariations: List<Long>? = null
}
