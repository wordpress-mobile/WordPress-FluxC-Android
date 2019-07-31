package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCVisitorStatsModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCVisitorStatsModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit

object WCVisitorStatsSqlUtils {
    fun insertOrUpdateVisitorStats(stats: WCVisitorStatsModel): Int {
        val statsResult = if (stats.isCustomField) {
            WellSql.select(WCVisitorStatsModel::class.java)
                    .where().beginGroup()
                    .equals(WCVisitorStatsModelTable.LOCAL_SITE_ID, stats.localSiteId)
                    .equals(WCVisitorStatsModelTable.UNIT, stats.unit)
                    .equals(WCVisitorStatsModelTable.DATE, stats.date)
                    .equals(WCVisitorStatsModelTable.QUANTITY, stats.quantity)
                    .equals(WCVisitorStatsModelTable.IS_CUSTOM_FIELD, stats.isCustomField)
                    .endGroup().endWhere()
                    .asModel
        } else {
            WellSql.select(WCVisitorStatsModel::class.java)
                    .where().beginGroup()
                    .equals(WCVisitorStatsModelTable.LOCAL_SITE_ID, stats.localSiteId)
                    .equals(WCVisitorStatsModelTable.UNIT, stats.unit)
                    .equals(WCVisitorStatsModelTable.IS_CUSTOM_FIELD, stats.isCustomField)
                    .endGroup().endWhere()
                    .asModel
        }

        if (statsResult.isEmpty()) {
            /*
             * if no visitor stats available for this particular date, quantity, unit and site:
             * - check if the incoming data is custom data or default data.
             *      - if custom data, we need to delete any previously cached custom data
             *        for the particular site before inserting incoming data
             */
            if (stats.isCustomField) {
                deleteCustomVisitorStatsForSite(stats.localSiteId)
            }

            WellSql.insert(stats).asSingleTransaction(true).execute()
            return 1
        } else {
            // Update
            val oldId = statsResult[0].id
            return WellSql.update(WCVisitorStatsModel::class.java).whereId(oldId)
                    .put(stats, UpdateAllExceptId(WCVisitorStatsModel::class.java)).execute()
        }
    }

    private fun getFirstRawStatsForSite(site: SiteModel): WCVisitorStatsModel? {
        return WellSql.select(WCVisitorStatsModel::class.java)
                .where()
                .equals(WCVisitorStatsModelTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .asModel.firstOrNull()
    }

    fun getRawVisitorStatsForSiteUnitQuantityAndDate(
        site: SiteModel,
        unit: OrderStatsApiUnit,
        quantity: String? = null,
        date: String? = null,
        isCustomField: Boolean = false
    ): WCVisitorStatsModel? {
        if (!isCustomField)
            return getRawVisitorStatsForSiteAndUnit(site, unit)

        return WellSql.select(WCVisitorStatsModel::class.java)
                .where()
                .beginGroup()
                .equals(WCVisitorStatsModelTable.LOCAL_SITE_ID, site.id)
                .equals(WCVisitorStatsModelTable.UNIT, unit)
                .equals(WCVisitorStatsModelTable.QUANTITY, quantity)
                .equals(WCVisitorStatsModelTable.DATE, date)
                .equals(WCVisitorStatsModelTable.IS_CUSTOM_FIELD, isCustomField)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    private fun getRawVisitorStatsForSiteAndUnit(site: SiteModel, unit: OrderStatsApiUnit): WCVisitorStatsModel? {
        return WellSql.select(WCVisitorStatsModel::class.java)
                .where()
                .beginGroup()
                .equals(WCVisitorStatsModelTable.LOCAL_SITE_ID, site.id)
                .equals(WCVisitorStatsModelTable.UNIT, unit)
                .equals(WCVisitorStatsModelTable.IS_CUSTOM_FIELD, false)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    private fun deleteCustomVisitorStatsForSite(siteId: Int): Int {
        return WellSql.delete(WCVisitorStatsModel::class.java)
                .where()
                .equals(WCVisitorStatsModelTable.LOCAL_SITE_ID, siteId)
                .equals(WCVisitorStatsModelTable.IS_CUSTOM_FIELD, true)
                .endWhere()
                .execute()
    }
}
