package org.wordpress.android.fluxc.model.product.attributes

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.product.attributes.terms.WCAttributeTermModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.AttributeApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.ProductAttributeRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.terms.AttributeTermApiResponse
import org.wordpress.android.fluxc.persistence.WCProductAttributeSqlUtils.insertAttributeTermsFromScratch
import javax.inject.Inject

class WCProductAttributeMapper @Inject constructor(
    private val restClient: ProductAttributeRestClient
) {
    suspend fun responseToAttributeModel(
        response: AttributeApiResponse,
        site: SiteModel
    ) = response.id?.toIntOrNull()?.let { attributeID ->
        restClient.fetchAllAttributeTerms(site, attributeID.toLong())
                .result?.map { responseToAttributeTermModel(it, attributeID, site) }
                ?.apply { insertAttributeTermsFromScratch(attributeID, site.id, this) }
                ?.map { it.id.toString() }
                ?.reduce { total, new -> "${total};${new}" }
                ?.let { terms ->
                    with(response) {
                        WCProductAttributeModel(
                                id = id?.toIntOrNull() ?: 0,
                                localSiteId = site.id,
                                name = name.orEmpty(),
                                slug = slug.orEmpty(),
                                type = type.orEmpty(),
                                orderBy = orderBy.orEmpty(),
                                hasArchives = hasArchives ?: false,
                                termsId = terms
                        )
                    }
                }
    }

    suspend fun responseToAttributeModelList(
        response: Array<AttributeApiResponse>,
        site: SiteModel
    ) = response.mapNotNull { responseToAttributeModel(it, site) }
            .toList()

    private fun responseToAttributeTermModel(
        response: AttributeTermApiResponse,
        attributeID: Int,
        site: SiteModel
    ) = with(response) {
        WCAttributeTermModel(
                id = id?.toIntOrNull() ?: 0,
                localSiteId = site.id,
                attributeId = attributeID,
                name = name.orEmpty(),
                slug = slug.orEmpty(),
                description = description.orEmpty(),
                count = count?.toIntOrNull() ?: 0,
                menuOrder = menuOrder?.toIntOrNull() ?: 0
        )
    }
}
