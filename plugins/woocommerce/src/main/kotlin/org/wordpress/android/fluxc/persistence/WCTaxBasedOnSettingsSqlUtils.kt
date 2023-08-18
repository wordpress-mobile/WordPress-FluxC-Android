package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCTaxBasedOnSettingsModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCTaxBasedOnSettingsModel

object WCTaxBasedOnSettingsSqlUtils {
    fun insertOrUpdateTaxBasedOnSettings(settings: WCTaxBasedOnSettingsModel): Int {
        val result = WellSql.select(WCTaxBasedOnSettingsModel::class.java)
                .where()
                .equals(WCTaxBasedOnSettingsModelTable.LOCAL_SITE_ID, settings.localSiteId)
                .endWhere()
                .asModel.firstOrNull()

        return if (result == null) {
            WellSql.insert(settings).asSingleTransaction(true).execute()
            1
        } else {
            val oldId = result.id
            WellSql.update(WCTaxBasedOnSettingsModel::class.java).whereId(oldId)
                    .put(settings, UpdateAllExceptId(WCTaxBasedOnSettingsModel::class.java)).execute()
        }
    }

    fun getTaxBasedOnSettingsForSite(site: SiteModel): WCTaxBasedOnSettingsModel? {
        return WellSql.select(WCTaxBasedOnSettingsModel::class.java)
                .where()
                .equals(WCTaxBasedOnSettingsModelTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .asModel.firstOrNull()
    }
}
