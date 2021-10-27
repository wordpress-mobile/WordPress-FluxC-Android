package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE, name = "WCOrderModel")
class BackwardCompatibilityWCOrderModel(@PrimaryKey @Column private val id: Int = 0) : Identifiable {
    override fun setId(id: Int) = Unit
    override fun getId(): Int = -1
}
