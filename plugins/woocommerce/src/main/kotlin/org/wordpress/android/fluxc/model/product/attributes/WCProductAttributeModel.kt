package org.wordpress.android.fluxc.model.product.attributes

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

data class WCProductAttributeModel(
    private var id: Int = 0,
    var name: String = "",
    var slug: String = "",
    var type: String = "",
    var orderBy: String = "",
    var hasArchives: Boolean = false
): Identifiable {
    override fun setId(id: Int) {
        this.id = id
    }

    override fun getId() = id
}
