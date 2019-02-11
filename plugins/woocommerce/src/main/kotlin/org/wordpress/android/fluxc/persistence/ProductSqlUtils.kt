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
                .equals(WCProductModelTable.REMOTE_PRODUCT_ID, product.remoteProductId)
                .equals(WCProductModelTable.LOCAL_SITE_ID, product.localSiteId)
                .endGroup()
                .endGroup().endWhere()
                .asModel

        return if (productResult.isEmpty()) {
            // Insert
            WellSql.insert(product).asSingleTransaction(true).execute()
            1
        } else {
            // Update
            val oldId = productResult[0].id
            WellSql.update(WCProductModel::class.java).whereId(oldId)
                    .put(product, UpdateAllExceptId(WCProductModel::class.java)).execute()
        }
    }

    fun getSingleProductByRemoteId(site: SiteModel, remoteProductId: Long): WCProductModel? {
        return WellSql.select(WCProductModel::class.java)
                .where().beginGroup()
                .equals(WCProductModelTable.REMOTE_PRODUCT_ID, remoteProductId)
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    fun deleteProductsForSite(site: SiteModel): Int {
        return WellSql.delete(WCProductModel::class.java)
                .where().beginGroup()
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .endGroup()
                .endWhere()
                .execute()
    }
}
