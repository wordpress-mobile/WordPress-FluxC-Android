package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCOrderModelTable
import com.wellsql.generated.WCOrderNoteModelTable
import com.wellsql.generated.WCOrderShipmentProviderModelTable
import com.wellsql.generated.WCOrderShipmentTrackingModelTable
import com.wellsql.generated.WCOrderStatusModelTable
import com.wellsql.generated.WCOrderSummaryModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.WCOrderSummaryModel
import org.wordpress.android.fluxc.model.order.OrderIdSet

object OrderSqlUtils {
    private const val CHUNK_SIZE = 200

    fun insertOrUpdateOrderSummaries(orderSummaries: List<WCOrderSummaryModel>) {
        WellSql.insert(orderSummaries).asSingleTransaction(true).execute()
    }

    /**
     * Returns a list of [WCOrderSummaryModel]s that match the [remoteOrderIds] provided. This method uses
     * Kotlin's chunked functionality to ensure we don't crash with the "SQLiteException: too many SQL variables"
     * exception.
     */
    fun getOrderSummariesForRemoteIds(site: SiteModel, remoteOrderIds: List<RemoteId>): List<WCOrderSummaryModel> {
        if (remoteOrderIds.isEmpty()) {
            return emptyList()
        }

        return remoteOrderIds.chunked(CHUNK_SIZE).map { doGetOrderSummariesForRemoteIds(site, it) }.flatten()
    }

    private fun doGetOrderSummariesForRemoteIds(
        site: SiteModel,
        remoteOrderIds: List<RemoteId>
    ): List<WCOrderSummaryModel> {
        return WellSql.select(WCOrderSummaryModel::class.java)
                .where()
                .equals(WCOrderSummaryModelTable.LOCAL_SITE_ID, site.id)
                .isIn(WCOrderSummaryModelTable.REMOTE_ORDER_ID, remoteOrderIds.map { it.value })
                .endWhere()
                .asModel
    }

    fun deleteOrderSummariesForSite(site: SiteModel) {
        WellSql.delete(WCOrderSummaryModel::class.java)
                .where()
                .equals(WCOrderSummaryModelTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .execute()
    }

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

    fun getOrderForIdSet(orderIdSet: OrderIdSet): WCOrderModel? {
        val (id, remoteOrderId, localSiteId) = orderIdSet
        return WellSql.select(WCOrderModel::class.java)
                .where().beginGroup()
                .equals(WCOrderModelTable.ID, id)
                .or()
                .beginGroup()
                .equals(WCOrderModelTable.REMOTE_ORDER_ID, remoteOrderId)
                .equals(WCOrderModelTable.LOCAL_SITE_ID, localSiteId)
                .endGroup()
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    fun getOrdersForSite(site: SiteModel, status: List<String> = emptyList()): List<WCOrderModel> {
        val conditionClauseBuilder = WellSql.select(WCOrderModel::class.java)
                .where()
                .beginGroup()
                .equals(WCOrderModelTable.LOCAL_SITE_ID, site.id)

        if (status.isNotEmpty()) {
            conditionClauseBuilder.isIn(WCOrderModelTable.STATUS, status)
        }

        return conditionClauseBuilder.endGroup().endWhere()
                .orderBy(WCOrderModelTable.DATE_CREATED, SelectQuery.ORDER_DESCENDING)
                .asModel
    }

    fun getOrdersForSiteByRemoteIds(site: SiteModel, remoteOrderIds: List<RemoteId>): List<WCOrderModel> {
        if (remoteOrderIds.isEmpty()) {
            return emptyList()
        }
        return WellSql.select(WCOrderModel::class.java)
                .where()
                .equals(WCOrderModelTable.LOCAL_SITE_ID, site.id)
                .isIn(WCOrderModelTable.REMOTE_ORDER_ID, remoteOrderIds.map { it.value })
                .endWhere()
                .asModel
    }

    fun deleteOrdersForSite(site: SiteModel): Int {
        return WellSql.delete(WCOrderModel::class.java)
                .where().beginGroup()
                .equals(WCOrderModelTable.LOCAL_SITE_ID, site.id)
                .endGroup()
                .endWhere()
                .execute()
    }

    fun getOrderCountForSite(site: SiteModel): Int {
        return WellSql.select(WCOrderModel::class.java)
                .where()
                .equals(WCOrderModelTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .count().toInt()
    }

    fun insertOrIgnoreOrderNotes(notes: List<WCOrderNoteModel>): Int {
        var totalChanged = 0
        notes.forEach { totalChanged += insertOrIgnoreOrderNote(it) }
        return totalChanged
    }

    fun insertOrIgnoreOrderNote(note: WCOrderNoteModel): Int {
        val noteResult = WellSql.select(WCOrderNoteModel::class.java)
                .where().beginGroup()
                .equals(WCOrderNoteModelTable.ID, note.id)
                .or()
                .beginGroup()
                .equals(WCOrderNoteModelTable.REMOTE_NOTE_ID, note.remoteNoteId)
                .equals(WCOrderNoteModelTable.LOCAL_SITE_ID, note.localSiteId)
                .equals(WCOrderNoteModelTable.LOCAL_ORDER_ID, note.localOrderId)
                .endGroup()
                .endGroup().endWhere()
                .asModel

        return if (noteResult.isEmpty()) {
            // Insert
            WellSql.insert(note).asSingleTransaction(true).execute()
            1
        } else {
            // Ignore
            0
        }
    }

    fun getOrderNotesForOrder(localId: Int): List<WCOrderNoteModel> =
            WellSql.select(WCOrderNoteModel::class.java)
                    .where()
                    .equals(WCOrderNoteModelTable.LOCAL_ORDER_ID, localId)
                    .endWhere()
                    .orderBy(WCOrderNoteModelTable.DATE_CREATED, SelectQuery.ORDER_DESCENDING)
                    .asModel

    fun deleteOrderNotesForSite(site: SiteModel): Int {
        return WellSql.delete(WCOrderNoteModel::class.java)
                .where()
                .equals(WCOrderNoteModelTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .execute()
    }

    fun insertOrUpdateOrderStatusOption(orderStatus: WCOrderStatusModel): Int {
        val result = WellSql.select(WCOrderStatusModel::class.java)
                .where().beginGroup()
                .equals(WCOrderStatusModelTable.ID, orderStatus.id)
                .or()
                .equals(WCOrderStatusModelTable.STATUS_KEY, orderStatus.statusKey)
                .endGroup().endWhere().asModel

        return if (result.isEmpty()) {
            // Insert
            WellSql.insert(orderStatus).asSingleTransaction(true).execute()
            1
        } else {
            // Update
            val oldId = result[0].id
            WellSql.update(WCOrderStatusModel::class.java).whereId(oldId)
                    .put(orderStatus, UpdateAllExceptId(WCOrderStatusModel::class.java)).execute()
        }
    }

    fun getOrderStatusOptionsForSite(site: SiteModel): List<WCOrderStatusModel> =
            WellSql.select(WCOrderStatusModel::class.java)
                    .where()
                    .equals(WCOrderStatusModelTable.LOCAL_SITE_ID, site.id)
                    .endWhere().asModel

    fun getOrderStatusOptionForSiteByKey(site: SiteModel, key: String): WCOrderStatusModel? =
            WellSql.select(WCOrderStatusModel::class.java)
                    .where().beginGroup()
                    .equals(WCOrderStatusModelTable.STATUS_KEY, key)
                    .equals(WCOrderStatusModelTable.LOCAL_SITE_ID, site.id)
                    .endGroup().endWhere().asModel.firstOrNull()

    fun deleteOrderStatusOption(orderStatus: WCOrderStatusModel): Int =
            WellSql.delete(WCOrderStatusModel::class.java).whereId(orderStatus.id)

    fun insertOrIgnoreOrderShipmentTracking(tracking: WCOrderShipmentTrackingModel): Int {
        val result = WellSql.select(WCOrderShipmentTrackingModel::class.java)
                .where().beginGroup()
                .equals(WCOrderShipmentTrackingModelTable.ID, tracking.id)
                .or()
                .beginGroup()
                .equals(WCOrderShipmentTrackingModelTable.LOCAL_SITE_ID, tracking.localSiteId)
                .equals(WCOrderShipmentTrackingModelTable.LOCAL_ORDER_ID, tracking.localOrderId)
                .equals(WCOrderShipmentTrackingModelTable.REMOTE_TRACKING_ID, tracking.remoteTrackingId)
                .endGroup().endGroup().endWhere().asModel

        return if (result.isEmpty()) {
            WellSql.insert(tracking).asSingleTransaction(true).execute()
            1
        } else {
            0
        }
    }

    fun getShipmentTrackingsForOrder(
        site: SiteModel,
        localOrderId: Int
    ): List<WCOrderShipmentTrackingModel> {
        return WellSql.select(WCOrderShipmentTrackingModel::class.java)
                .where()
                .beginGroup()
                .equals(WCOrderShipmentTrackingModelTable.LOCAL_SITE_ID, site.id)
                .equals(WCOrderShipmentTrackingModelTable.LOCAL_ORDER_ID, localOrderId)
                .endGroup().endWhere()
                .orderBy(WCOrderShipmentTrackingModelTable.DATE_SHIPPED, SelectQuery.ORDER_DESCENDING).asModel
    }

    fun getShipmentTrackingByTrackingNumber(
        site: SiteModel,
        localOrderId: Int,
        trackingNumber: String
    ): WCOrderShipmentTrackingModel? {
        return WellSql.select(WCOrderShipmentTrackingModel::class.java)
                .where()
                .beginGroup()
                .equals(WCOrderShipmentTrackingModelTable.LOCAL_SITE_ID, site.id)
                .equals(WCOrderShipmentTrackingModelTable.LOCAL_ORDER_ID, localOrderId)
                .equals(WCOrderShipmentTrackingModelTable.TRACKING_NUMBER, trackingNumber)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    fun deleteOrderShipmentTrackingById(tracking: WCOrderShipmentTrackingModel): Int =
            WellSql.delete(WCOrderShipmentTrackingModel::class.java).whereId(tracking.id)

    fun deleteOrderShipmentTrackingsForSite(site: SiteModel): Int =
            WellSql.delete(WCOrderShipmentTrackingModel::class.java)
                    .where()
                    .equals(WCOrderShipmentTrackingModelTable.LOCAL_SITE_ID, site.id)
                    .endWhere()
                    .execute()

    fun deleteOrderShipmentProvidersForSite(site: SiteModel): Int =
            WellSql.delete(WCOrderShipmentProviderModel::class.java)
                    .where()
                    .equals(WCOrderShipmentProviderModelTable.LOCAL_SITE_ID, site.id)
                    .endWhere()
                    .execute()

    fun insertOrIgnoreOrderShipmentProvider(provider: WCOrderShipmentProviderModel): Int {
        val result = WellSql.select(WCOrderShipmentProviderModel::class.java)
                .where().beginGroup()
                .equals(WCOrderShipmentProviderModelTable.ID, provider.id)
                .or()
                .beginGroup()
                .equals(WCOrderShipmentProviderModelTable.LOCAL_SITE_ID, provider.localSiteId)
                .equals(WCOrderShipmentProviderModelTable.CARRIER_NAME, provider.carrierName)
                .endGroup()
                .endGroup().endWhere()
                .asModel

        return if (result.isEmpty()) {
            // Insert
            WellSql.insert(provider).asSingleTransaction(true).execute()
            1
        } else {
            // Ignore
            0
        }
    }

    fun getOrderShipmentProvidersForSite(site: SiteModel): List<WCOrderShipmentProviderModel> =
            WellSql.select(WCOrderShipmentProviderModel::class.java)
                    .where()
                    .equals(WCOrderShipmentProviderModelTable.LOCAL_SITE_ID, site.id)
                    .endWhere()
                    .orderBy(WCOrderShipmentProviderModelTable.COUNTRY, SelectQuery.ORDER_ASCENDING)
                    .asModel

    fun getOrderByLocalId(localOrderId: Int): WCOrderModel =
            WellSql.select(WCOrderModel::class.java)
                    .where()
                    .equals(WCOrderModelTable.ID, localOrderId)
                    .endWhere()
                    .asModel
                    .first()

    fun getOrderByLocalIdOrNull(localOrderId: LocalId): WCOrderModel? =
            WellSql.select(WCOrderModel::class.java)
                    .where()
                    .equals(WCOrderModelTable.ID, localOrderId.value)
                    .endWhere()
                    .asModel
                    .firstOrNull()
}
