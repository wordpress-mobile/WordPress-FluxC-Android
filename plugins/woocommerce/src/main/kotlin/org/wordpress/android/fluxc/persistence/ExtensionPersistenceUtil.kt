package org.wordpress.android.fluxc.persistence

import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductApiResponse
import org.wordpress.android.fluxc.persistence.dao.BundledProductsDao
import org.wordpress.android.fluxc.persistence.mappers.BundledProductMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtensionPersistenceUtil @Inject constructor(
    private val bundledProductsDao: BundledProductsDao
) {
    suspend fun persistExtensionData(localSiteId: Int, productApiResponse: ProductApiResponse) {
        when (productApiResponse.type) {
            CoreProductType.BUNDLE.value -> {
                val productId = productApiResponse.id
                val bundledProducts = BundledProductMapper.toDatabaseEntityList(
                    localSiteId = localSiteId,
                    productId = productId,
                    jsonArray = productApiResponse.bundled_items
                )

                if(productId == null || bundledProducts == null) return

                bundledProductsDao.updateBundledProductsFor(
                    localSiteId = LocalOrRemoteId.LocalId(localSiteId),
                    productId = LocalOrRemoteId.RemoteId(productId),
                    bundledProducts = bundledProducts
                )
            }
        }
    }
}
