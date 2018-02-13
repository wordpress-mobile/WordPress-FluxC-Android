package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCOrderModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel

object OrderSqlUtils {
    fun insertOrUpdateOrder(order: WCOrderModel): Int {
        val orderResult = WellSql.select(WCOrderModel::class.java)
                .where().beginGroup()
                .equals(WCOrderModelTable.ID, order.id)
                .or()
                .beginGroup()
                .equals(WCOrderModelTable.REMOTE_ORDER_ID, order.remoteOrderId)
                .equals(WCOrderModelTable.LOCAL_SITE_ID, order.localSiteId)
                .endGroup()
                .endGroup().endWhere()
                .asModel

        if (orderResult.isEmpty()) {
            // Insert
            WellSql.insert(order).asSingleTransaction(true).execute()
            return 1
        } else {
            // Update
            val oldId = orderResult[0].id
            return WellSql.update(WCOrderModel::class.java).whereId(oldId)
                    .put(order, UpdateAllExceptId(WCOrderModel::class.java)).execute()
        }
    }

    fun getOrdersForSite(site: SiteModel): List<WCOrderModel> {
        return WellSql.select(WCOrderModel::class.java)
                .where()
                .equals(WCOrderModelTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .orderBy(WCOrderModelTable.DATE_CREATED, SelectQuery.ORDER_DESCENDING)
                .asModel
    }

    fun getOrdersForSiteWithStatus(site: SiteModel, status: List<String>): List<WCOrderModel> {
        return if (status.isEmpty()) {
            getOrdersForSite(site)
        } else {
            WellSql.select(WCOrderModel::class.java)
                    .where().beginGroup()
                    .equals(WCOrderModelTable.LOCAL_SITE_ID, site.id)
                    .isIn(WCOrderModelTable.STATUS, status)
                    .endGroup().endWhere()
                    .orderBy(WCOrderModelTable.DATE_CREATED, SelectQuery.ORDER_DESCENDING)
                    .asModel
        }
    }

    fun deleteOrdersForSite(site: SiteModel): Int {
        return WellSql.delete(WCOrderModel::class.java)
                .where().beginGroup()
                .equals(WCOrderModelTable.LOCAL_SITE_ID, site.id)
                .endGroup()
                .endWhere()
                .execute()
    }
}
