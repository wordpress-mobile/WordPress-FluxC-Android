package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCProductModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel

object ProductSqlUtils {
    fun insertOrUpdateProduct(product: WCProductModel): Int {
        val productResult = WellSql.select(WCProductModel::class.java)
                .where().beginGroup()
                .equals(WCProductModelTable.ID, product.id)
                .or()
                .beginGroup()
                .equals(WCProductModelTable.LOCAL_SITE_ID, product.localSiteId)
                .equals(WCProductModelTable.REMOTE_PRODUCT_ID, product.remoteProductId)
                .equals(WCProductModelTable.REMOTE_VARIATION_ID, product.remoteVariationId)
                .endGroup()
                .endGroup().endWhere()
                .asModel.firstOrNull()

        return if (productResult == null) {
            // Insert
            WellSql.insert(product).asSingleTransaction(true).execute()
            1
        } else {
            // Update
            val oldId = productResult.id
            WellSql.update(WCProductModel::class.java).whereId(oldId)
                    .put(product, UpdateAllExceptId(WCProductModel::class.java)).execute()
        }
    }

    fun getProductByRemoteId(site: SiteModel, remoteProductId: Long, remoteVariationId: Long = 0): WCProductModel? {
        return WellSql.select(WCProductModel::class.java)
                .where().beginGroup()
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .equals(WCProductModelTable.REMOTE_PRODUCT_ID, remoteProductId)
                .equals(WCProductModelTable.REMOTE_VARIATION_ID, remoteVariationId)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    fun geProductExistsByRemoteId(site: SiteModel, remoteProductId: Long, remoteVariationId: Long = 0): Boolean {
        return WellSql.select(WCProductModel::class.java)
                .where().beginGroup()
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .equals(WCProductModelTable.REMOTE_PRODUCT_ID, remoteProductId)
                .equals(WCProductModelTable.REMOTE_VARIATION_ID, remoteVariationId)
                .endGroup().endWhere()
                .exists()
    }

    fun deleteProductsForSite(site: SiteModel): Int {
        return WellSql.delete(WCProductModel::class.java)
                .where().beginGroup()
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .endGroup()
                .endWhere()
                .execute()
    }

    fun getProductCountForSite(site: SiteModel, excludeVariations: Boolean = true): Long {
        return if (excludeVariations) {
            WellSql.select(WCProductModel::class.java)
                    .where()
                    .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                    .equals(WCProductModelTable.REMOTE_VARIATION_ID, 0L)
                    .endWhere()
                    .count()
        } else {
            WellSql.select(WCProductModel::class.java)
                    .where()
                    .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                    .endWhere()
                    .count()
        }
    }
}
