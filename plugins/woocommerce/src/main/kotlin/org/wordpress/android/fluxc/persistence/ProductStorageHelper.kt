package org.wordpress.android.fluxc.persistence

import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.ProductWithMetaData
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.dao.MetaDataDao
import org.wordpress.android.fluxc.persistence.entity.MetaDataEntity
import javax.inject.Inject

class ProductStorageHelper @Inject constructor(
    private val productSqlUtils: ProductSqlUtils,
    private val metaDataDao: MetaDataDao
) {
    suspend fun upsertProduct(productWithMetaData: ProductWithMetaData): Int {
        val (product, metadata) = productWithMetaData
        val rowsAffected = productSqlUtils.insertOrUpdateProduct(product)

        metaDataDao.updateMetaData(
            parentItemId = product.remoteProductId,
            localSiteId = LocalId(product.localSiteId),
            metaData = metadata.map {
                MetaDataEntity(
                    localSiteId = LocalId(product.localSiteId),
                    id = it.id,
                    parentItemId = product.remoteProductId,
                    key = it.key,
                    value = it.valueAsString,
                    type = MetaDataEntity.ParentItemType.PRODUCT
                )
            }
        )
        return rowsAffected
    }

    suspend fun upsertProducts(productsWithMetaData: List<ProductWithMetaData>): Int {
        val products = productsWithMetaData.map { it.product }
        val rowsAffected = productSqlUtils.insertOrUpdateProducts(products)

        productsWithMetaData.forEach { productWithMetaData ->
            val (product, metadata) = productWithMetaData
            metaDataDao.updateMetaData(
                parentItemId = product.remoteProductId,
                localSiteId = LocalId(product.localSiteId),
                metaData = metadata.map {
                    MetaDataEntity(
                        localSiteId = LocalId(product.localSiteId),
                        id = it.id,
                        parentItemId = product.remoteProductId,
                        key = it.key,
                        value = it.valueAsString,
                        type = MetaDataEntity.ParentItemType.PRODUCT
                    )
                }
            )
        }

        return rowsAffected
    }

    suspend fun deleteProduct(site: SiteModel, remoteProductId: Long): Int {
        val rowsAffected = productSqlUtils.deleteProduct(site, remoteProductId)
        metaDataDao.deleteMetaData(site.localId(), remoteProductId)
        return rowsAffected
    }

    suspend fun deleteProductsForSite(site: SiteModel) {
        productSqlUtils.deleteProductsForSite(site)
        metaDataDao.deleteMetaDataForSite(site.localId(), MetaDataEntity.ParentItemType.PRODUCT)
    }
}
