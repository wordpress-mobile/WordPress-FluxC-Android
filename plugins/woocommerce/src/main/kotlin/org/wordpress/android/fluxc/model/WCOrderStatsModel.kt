package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCOrderStatsModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column var unit = "" // The unit ("day", "week", "month", "year")
    @Column var fields = "" // JSON - A map of numerical index to stat name, used to lookup the stat in the data object
    @Column var data = "" // JSON - A list of lists; each nested list contains the data for a time period

    companion object {
        private val gson by lazy { Gson() }
    }

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }

    /**
     * Deserializes the JSON contained in [fields] into a list of strings.
     */
    fun getFieldsList(): List<String> {
        val responseType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(fields, responseType) as? List<String> ?: emptyList()
    }

    /**
     * Deserializes the JSON contained in [data] into a list of lists of arbitrary type.
     */
    fun getDataList(): List<List<Any>> {
        val responseType = object : TypeToken<List<List<Any>>>() {}.type
        return gson.fromJson(data, responseType) as? List<List<Any>> ?: emptyList()
    }
}
