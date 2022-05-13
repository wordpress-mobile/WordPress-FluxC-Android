package org.wordpress.android.fluxc.utils

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.dao.ProductVariationsDao
import org.wordpress.android.fluxc.persistence.entity.toDataModel
import javax.inject.Inject

class ProductVariationsDbHelper @Inject constructor(
    private val dao: ProductVariationsDao
) {
    suspend fun deleteAllProductVariationsForProduct(site: SiteModel, productId: Long) {
        ProductSqlUtils.deleteVariationsForProduct(site, productId)
        dao.deleteAllProductVariationsForProduct(site.siteId, productId)
    }

    suspend fun insertOrUpdateProductVariations(
        site: SiteModel,
        vararg variations: WCProductVariationModel
    ): Int {
        return this.insertOrUpdateProductVariations(site, variations.asList())
    }

    suspend fun insertOrUpdateProductVariations(
        site: SiteModel,
        variations: List<WCProductVariationModel>
    ): Int {
        dao.insertOrUpdateProductVariations(variations.map { variation ->
            variation.toDataModel(site.siteId)
        })

        return ProductSqlUtils.insertOrUpdateProductVariations(variations)
    }
}
