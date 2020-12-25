package org.wordpress.android.fluxc.model.product.attributes

import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.AttributeApiResponse
import javax.inject.Inject

class WCProductAttributeMapper @Inject constructor() {
    fun mapToAttributeModel(
        response: AttributeApiResponse
    ) = with(response) {
        WCProductAttributeModel(
                id = id?.toIntOrNull() ?: 0,
                name = name.orEmpty(),
                slug = slug.orEmpty(),
                type = type.orEmpty(),
                orderBy = orderBy.orEmpty(),
                hasArchives = hasArchives ?: false
        )
    }

    fun mapToAttributeModelList(
        response: Array<AttributeApiResponse>
    ) = response.map { mapToAttributeModel(it) }
            .toList()
}
