package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.persistence.dao.ProductCategoriesDao
import org.wordpress.android.fluxc.persistence.entity.ProductCategoryEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductCategoryStore @Inject constructor(
    private val productCategoriesDao: ProductCategoriesDao
) {
    suspend fun getCategories(siteId: Long): List<ProductCategoryEntity> =
        productCategoriesDao.getCategories(siteId)
}
