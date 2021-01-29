package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCSettingsModelTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition

object WCSettingsSqlUtils {
    fun insertOrUpdateSettings(settings: WCSettingsModel): Int {
        val orderResult = WellSql.select(WCSettingsBuilder::class.java)
                .where()
                .equals(WCSettingsModelTable.LOCAL_SITE_ID, settings.localSiteId)
                .endWhere()
                .asModel

        return if (orderResult.isEmpty()) {
            // Insert
            WellSql.insert(settings.toBuilder()).asSingleTransaction(true).execute()
            1
        } else {
            // Update
            val oldId = orderResult[0].id
            WellSql.update(WCSettingsBuilder::class.java).whereId(oldId)
                    .put(settings.toBuilder(), UpdateAllExceptId(WCSettingsBuilder::class.java)).execute()
        }
    }

    fun getSettingsForSite(site: SiteModel): WCSettingsModel? {
        return WellSql.select(WCSettingsBuilder::class.java)
                .where()
                .equals(WCSettingsModelTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .asModel.firstOrNull()?.build()
    }

    @Table(name = "WCSettingsModel", addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
    data class WCSettingsBuilder(
        @PrimaryKey @Column private var id: Int = 0,
        @Column var localSiteId: Int = 0,
        @Column var currencyCode: String = "",
        @Column var currencyPosition: String = "",
        @Column var currencyThousandSeparator: String = "",
        @Column var currencyDecimalSeparator: String = "",
        @Column var currencyDecimalNumber: Int = 2,
        @Column var countryCode: String = "",
        @Column var stateCode: String = "",
        @Column var address: String = "",
        @Column var address2: String = "",
        @Column var city: String = "",
        @Column var postalCode: String = ""
    ) : Identifiable {
        override fun getId() = id

        override fun setId(id: Int) {
            this.id = id
        }

        fun build(): WCSettingsModel {
            return WCSettingsModel(
                    localSiteId = localSiteId,
                    currencyCode = currencyCode,
                    currencyPosition = CurrencyPosition.fromString(currencyPosition),
                    currencyThousandSeparator = currencyThousandSeparator,
                    currencyDecimalSeparator = currencyDecimalSeparator,
                    currencyDecimalNumber = currencyDecimalNumber,
                    countryCode = countryCode,
                    stateCode = stateCode,
                    address = address,
                    address2 = address2,
                    city = city,
                    postalCode = postalCode
            )
        }
    }

    private fun WCSettingsModel.toBuilder(): WCSettingsBuilder {
        return WCSettingsBuilder(
                localSiteId = this.localSiteId,
                currencyCode = this.currencyCode,
                currencyPosition = this.currencyPosition.name.toLowerCase(),
                currencyThousandSeparator = this.currencyThousandSeparator,
                currencyDecimalSeparator = this.currencyDecimalSeparator,
                currencyDecimalNumber = this.currencyDecimalNumber,
                countryCode = countryCode,
                stateCode = stateCode,
                address = address,
                address2 = address2,
                city = city,
                postalCode = postalCode
        )
    }
}
