package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE, name = "WCOrderModel")
class BackwardCompatibilityWCOrderModel : Identifiable {
    override fun setId(id: Int) {
        //no-op
    }

    override fun getId(): Int {
        //no-op
        return -1
    }
}
