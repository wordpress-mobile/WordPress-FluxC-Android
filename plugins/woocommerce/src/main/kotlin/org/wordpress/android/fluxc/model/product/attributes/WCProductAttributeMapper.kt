package org.wordpress.android.fluxc.model.product.attributes

import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.AttributeApiResponse

class WCProductAttributeMapper {
    fun map(
        response: AttributeApiResponse
    ) = WCProductAttributeModel(
            id = response.id?.toIntOrNull() ?: 0,
            name = response.name.orEmpty(),
            slug = response.slug.orEmpty(),
            type = response.slug.orEmpty(),
            orderBy = response.orderBy.orEmpty(),
            hasArchives = response.hasArchives ?: false
    )
}
