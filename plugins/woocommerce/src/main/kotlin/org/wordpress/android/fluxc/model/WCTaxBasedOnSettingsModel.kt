package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.network.utils.toMap
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

    val availableOptionList: List<TaxOption>
        get() = gson.fromJson(availableOptions, JsonObject::class.java)
            .toMap()
            .toList()
            .map { TaxOption(it.first, it.second.toString()) }

    data class TaxOption(
        val key: String,
        val label: String,
    )
}
