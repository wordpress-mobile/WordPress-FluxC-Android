package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCOrderStatsModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderStatsModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit

object WCStatsSqlUtils {
    fun insertOrUpdateStats(stats: WCOrderStatsModel): Int {
        val statsResult = if(stats.isCustomField) {
            WellSql.select(WCOrderStatsModel::class.java)
                    .where().beginGroup()
                    .equals(WCOrderStatsModelTable.LOCAL_SITE_ID, stats.localSiteId)
                    .equals(WCOrderStatsModelTable.UNIT, stats.unit)
                    .equals(WCOrderStatsModelTable.DATE, stats.date)
                    .equals(WCOrderStatsModelTable.QUANTITY, stats.quantity)
                    .equals(WCOrderStatsModelTable.IS_CUSTOM_FIELD, stats.isCustomField)
                    .endGroup().endWhere()
                    .asModel
        } else {
            WellSql.select(WCOrderStatsModel::class.java)
                    .where().beginGroup()
                    .equals(WCOrderStatsModelTable.LOCAL_SITE_ID, stats.localSiteId)
                    .equals(WCOrderStatsModelTable.UNIT, stats.unit)
                    .equals(WCOrderStatsModelTable.IS_CUSTOM_FIELD, stats.isCustomField)
                    .endGroup().endWhere()
                    .asModel
        }

        if (statsResult.isEmpty()) {
            /*
             * if no stats available for this particular date, quantity, unit and site:
             * - check if the incoming data is custom data or default data.
             *      - if custom data, we need to delete any previously cached custom data
             *        for the particular site before inserting incoming data
             */
            if (stats.isCustomField) {
                deleteCustomStatsForSite(stats.localSiteId)
            }

            WellSql.insert(stats).asSingleTransaction(true).execute()
            return 1
        } else {
            // Update
            val oldId = statsResult[0].id
            return WellSql.update(WCOrderStatsModel::class.java).whereId(oldId)
                    .put(stats, UpdateAllExceptId(WCOrderStatsModel::class.java)).execute()
        }
    }

    fun getFirstRawStatsForSite(site: SiteModel): WCOrderStatsModel? {
        return WellSql.select(WCOrderStatsModel::class.java)
                .where()
                .equals(WCOrderStatsModelTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .asModel.firstOrNull()
    }

    fun getRawStatsForSiteUnitQuantityAndDate(
        site: SiteModel,
        unit: OrderStatsApiUnit,
        quantity: String? = null,
        date: String? = null,
        isCustomField: Boolean = false
    ): WCOrderStatsModel? {
        if (!isCustomField)
                return getRawStatsForSiteAndUnit(site, unit)

        return WellSql.select(WCOrderStatsModel::class.java)
                .where()
                .beginGroup()
                .equals(WCOrderStatsModelTable.LOCAL_SITE_ID, site.id)
                .equals(WCOrderStatsModelTable.UNIT, unit)
                .equals(WCOrderStatsModelTable.QUANTITY, quantity)
                .equals(WCOrderStatsModelTable.DATE, date)
                .equals(WCOrderStatsModelTable.IS_CUSTOM_FIELD, isCustomField)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    fun getCustomStatsForSite(
        site: SiteModel
    ): WCOrderStatsModel? {
        return WellSql.select(WCOrderStatsModel::class.java)
                .where()
                .beginGroup()
                .equals(WCOrderStatsModelTable.LOCAL_SITE_ID, site.id)
                .equals(WCOrderStatsModelTable.IS_CUSTOM_FIELD, true)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    private fun getRawStatsForSiteAndUnit(site: SiteModel, unit: OrderStatsApiUnit): WCOrderStatsModel? {
        return WellSql.select(WCOrderStatsModel::class.java)
                .where()
                .beginGroup()
                .equals(WCOrderStatsModelTable.LOCAL_SITE_ID, site.id)
                .equals(WCOrderStatsModelTable.UNIT, unit)
                .equals(WCOrderStatsModelTable.IS_CUSTOM_FIELD, false)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    private fun deleteCustomStatsForSite(siteId: Int): Int {
        return WellSql.delete(WCOrderStatsModel::class.java)
                .where()
                .equals(WCOrderStatsModelTable.LOCAL_SITE_ID, siteId)
                .equals(WCOrderStatsModelTable.IS_CUSTOM_FIELD, true)
                .endWhere()
                .execute()
    }
}
