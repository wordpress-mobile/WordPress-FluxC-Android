package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import com.google.gson.annotations.SerializedName

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCRevenueStatsModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column var interval = "" // The unit ("hour", "day", "week", "month", "year")
    @Column var startDate = "" // The start date of the data
    @Column var endDate = "" // The end date of the data
    @Column var data = "" // JSON - A list of lists; each nested list contains the data for a time period

    companion object {
        private val gson by lazy { Gson() }
    }

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }

    class Interval {
        val interval: String? = null
        val subtotals: SubTotal? = null
    }

    class SubTotal {
        @SerializedName("orders_count")
        val ordersCount: Long? = null
        @SerializedName("gross_revenue")
        val grossRevenue: Double? = null
    }

    /**
     * Deserializes the JSON contained in [data] into a list of [Interval] objects.
     */
    fun getIntervalList(): List<Interval> {
        val responseType = object : TypeToken<List<Interval>>() {}.type
        return gson.fromJson(data, responseType) as? List<Interval> ?: emptyList()
    }
}
