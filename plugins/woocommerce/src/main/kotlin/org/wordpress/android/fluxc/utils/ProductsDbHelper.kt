package org.wordpress.android.fluxc.utils

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.dao.ProductsDao
import org.wordpress.android.fluxc.persistence.entity.toDataModel
import javax.inject.Inject

class ProductsDbHelper @Inject constructor(
    private val dao: ProductsDao
) {
    suspend fun deleteAllProducts(site: SiteModel) {
        ProductSqlUtils.deleteProductsForSite(site)
        dao.deleteAllProducts(site.siteId)
    }

    suspend fun deleteProduct(site: SiteModel, productId: Long): Int {
        dao.deleteProductByProductId(site.siteId, productId)
        return ProductSqlUtils.deleteProduct(site, productId)
    }

    suspend fun insertOrUpdateProducts(site: SiteModel, vararg products: WCProductModel): Int {
        return insertOrUpdateProducts(site, products.asList())
    }

    suspend fun insertOrUpdateProducts(site: SiteModel, products: List<WCProductModel>): Int {
        dao.insertOrUpdateProducts(products.map { product ->
            product.toDataModel(site.siteId)
        })

        return ProductSqlUtils.insertOrUpdateProducts(products)
    }
}
