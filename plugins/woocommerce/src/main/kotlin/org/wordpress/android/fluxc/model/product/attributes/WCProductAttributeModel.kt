package org.wordpress.android.fluxc.model.product.attributes

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCProductAttributeModel(
    @PrimaryKey @Column private var id: Int = 0,
    @Column var name: String = "",
    @Column var slug: String = "",
    @Column var type: String = "",
    @Column var orderBy: String = "",
    @Column var hasArchives: Boolean = false
): Identifiable {
    override fun setId(id: Int) {
        this.id = id
    }

    override fun getId() = id
}
