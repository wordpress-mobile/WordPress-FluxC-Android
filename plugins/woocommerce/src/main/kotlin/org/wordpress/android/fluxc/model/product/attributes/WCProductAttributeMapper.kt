package org.wordpress.android.fluxc.model.product.attributes

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.AttributeApiResponse
import javax.inject.Inject

class WCProductAttributeMapper @Inject constructor() {
    fun mapToAttributeModel(
        response: AttributeApiResponse,
        site: SiteModel
    ) = with(response) {
        WCProductAttributeModel(
                id = id?.toIntOrNull() ?: 0,
                localSiteId = site.id,
                name = name.orEmpty(),
                slug = slug.orEmpty(),
                type = type.orEmpty(),
                orderBy = orderBy.orEmpty(),
                hasArchives = hasArchives ?: false
        )
    }

    fun mapToAttributeModelList(
        response: Array<AttributeApiResponse>,
        site: SiteModel
    ) = response.map { mapToAttributeModel(it, site) }
            .toList()
}
