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

    // The fields JSON deserialized into a list of strings
    val fieldsList by lazy {
        val responseType = object : TypeToken<List<String>>() {}.type
        gson.fromJson(fields, responseType) as? List<String> ?: emptyList()
    }

    // The data JSON deserialized into a list of lists of arbitrary type
    val dataList by lazy {
        val responseType = object : TypeToken<List<List<Any>>>() {}.type
        gson.fromJson(data, responseType) as? List<List<Any>> ?: emptyList()
    }

    companion object {
        private val gson by lazy { Gson() }
    }

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }
}
