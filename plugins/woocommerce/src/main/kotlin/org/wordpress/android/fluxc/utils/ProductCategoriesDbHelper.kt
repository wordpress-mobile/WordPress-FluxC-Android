package org.wordpress.android.fluxc.utils

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.dao.ProductCategoriesDao
import org.wordpress.android.fluxc.persistence.entity.toDataModel
import javax.inject.Inject

class ProductCategoriesDbHelper @Inject constructor(
    private val dao: ProductCategoriesDao
) {
    suspend fun deleteAllProductCategories(site: SiteModel) {
        ProductSqlUtils.deleteAllProductCategoriesForSite(site)
        dao.deleteAllProductCategories(site.siteId)
    }

    suspend fun insertOrUpdateProductCategories(
        site: SiteModel,
        vararg categories: WCProductCategoryModel
    ): Int {
        return insertOrUpdateProductCategories(site, categories.asList())
    }

    suspend fun insertOrUpdateProductCategories(
        site: SiteModel,
        categories: List<WCProductCategoryModel>
    ): Int {
        dao.insertOrUpdateProductCategories(categories.map { category ->
            category.toDataModel(site.siteId)
        })

        return ProductSqlUtils.insertOrUpdateProductCategories(categories)
    }
}
