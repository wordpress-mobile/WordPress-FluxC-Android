package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCShippingLabelModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel

object WCShippingLabelSqlUtils {
    fun getShippingClassesForOrder(
        localSiteId: Int,
        orderId: Long
    ): List<WCShippingLabelModel> {
        return WellSql.select(WCShippingLabelModel::class.java)
                .where()
                .equals(WCShippingLabelModelTable.LOCAL_SITE_ID, localSiteId)
                .equals(WCShippingLabelModelTable.LOCAL_ORDER_ID, orderId)
                .endWhere()
                .asModel
    }

    fun insertOrUpdateShippingLabels(shippingLabels: List<WCShippingLabelModel>): Int {
        var totalChanged = 0
        shippingLabels.forEach { totalChanged += insertOrUpdateShippingLabel(it) }
        return totalChanged
    }

    fun insertOrUpdateShippingLabel(shippingLabel: WCShippingLabelModel): Int {
        val orderResult = WellSql.select(WCShippingLabelModel::class.java)
                .where().beginGroup()
                .equals(WCShippingLabelModelTable.ID, shippingLabel.id)
                .or()
                .beginGroup()
                .equals(WCShippingLabelModelTable.REMOTE_SHIPPING_LABEL_ID, shippingLabel.remoteShippingLabelId)
                .equals(WCShippingLabelModelTable.LOCAL_ORDER_ID, shippingLabel.localOrderId)
                .equals(WCShippingLabelModelTable.LOCAL_SITE_ID, shippingLabel.localSiteId)
                .endGroup()
                .endGroup().endWhere()
                .asModel

        return if (orderResult.isEmpty()) {
            // Insert
            WellSql.insert(shippingLabel).asSingleTransaction(true).execute()
            1
        } else {
            // Update
            val oldId = orderResult[0].id
            WellSql.update(WCShippingLabelModel::class.java).whereId(oldId)
                    .put(shippingLabel, UpdateAllExceptId(WCShippingLabelModel::class.java)).execute()
        }
    }

    fun deleteShippingLabelsForOrder(orderId: Long): Int =
            WellSql.delete(WCShippingLabelModel::class.java)
                    .where()
                    .equals(WCShippingLabelModelTable.LOCAL_ORDER_ID, orderId)
                    .endWhere().execute()

    fun deleteShippingLabelsForSite(localSiteId: Int): Int {
        return WellSql.delete(WCShippingLabelModel::class.java)
                .where()
                .equals(WCShippingLabelModelTable.LOCAL_SITE_ID, localSiteId)
                .or()
                .equals(WCShippingLabelModelTable.LOCAL_SITE_ID, 0) // Should never happen, but sanity cleanup
                .endWhere().execute()
    }
}
