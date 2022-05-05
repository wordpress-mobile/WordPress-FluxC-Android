package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.persistence.dao.ProductsDao
import org.wordpress.android.fluxc.persistence.entity.ProductEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductStore @Inject constructor(
    private val productsDao: ProductsDao
) {
    suspend fun getProducts(siteId: Long): List<ProductEntity> =
        productsDao.getProducts(siteId)
}
