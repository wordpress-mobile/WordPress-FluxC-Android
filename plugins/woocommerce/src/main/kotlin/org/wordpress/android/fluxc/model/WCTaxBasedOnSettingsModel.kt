package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

/**
 * @param availableOptions JSON array of key-value entries
 * @param selectedOption Key of the selected option
 */
@Table(name = "WCTaxBasedOnSettingsModel", addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCTaxBasedOnSettingsModel(
    @PrimaryKey @Column private var id: Int = 0,
    @Column var localSiteId: Int = 0,
    @Column var availableOptions: String = "",
    @Column var selectedOption: String = "",
) : Identifiable {
    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }

    private val gson by lazy { Gson() }

    val availableOptionList: Array<TaxOption> =
        gson.fromJson(availableOptions, Array<TaxOption>::class.java)

    data class TaxOption(
        val key: String,
        val label: String,
    )
}
