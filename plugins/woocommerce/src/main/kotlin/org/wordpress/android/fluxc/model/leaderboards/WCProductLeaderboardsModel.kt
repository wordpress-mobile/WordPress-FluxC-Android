package org.wordpress.android.fluxc.model.leaderboards

import com.google.gson.Gson
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCProductLeaderboardsModel(
    @PrimaryKey @Column private var id: Int = 0,
    @Column var products: String = ""
) : Identifiable {
    val productList by lazy { Gson().fromJson(products, Array<WCProductModel>::class.java).toList() }

    override fun setId(id: Int) {
        this.id = id
    }

    override fun getId() = id
}
